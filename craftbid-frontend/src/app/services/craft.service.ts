import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { getApiBaseUrl } from './api-config';
import { Trie } from '../core/dsa/trie';
import { LRUCache } from '../core/dsa/lru-cache';

export interface CraftItem {
  id: number;
  title: string;
  description: string;
  basePrice: number;
  imageUrl: string;
  status: string;
  seller?: {
    id: number;
    name: string;
    email: string;
    phone?: string;
  };
  category?: {
    id: number;
    name: string;
    description?: string;
    imageUrl?: string;
  };
  createdAt: string;
}

export interface AutocompleteItem {
  suggestion: string;
  matchCount: number;
}

@Injectable({
  providedIn: 'root',
})
export class CraftService {
  private apiUrl = `${getApiBaseUrl()}/api/crafts`;

  // DSA: Client-side LRU Cache for search query results (5-minute TTL)
  private searchCache = new LRUCache<string, CraftItem[]>(40, 300_000);

  // DSA: Client-side Trie index for sub-millisecond local autocomplete
  private localSearchTrie = new Trie<CraftItem>();

  constructor(private http: HttpClient) {}

  uploadCraft(formData: FormData): Observable<CraftItem> {
    return this.http.post<CraftItem>(`${this.apiUrl}/upload`, formData).pipe(
      tap((craft) => {
        this.indexCraftLocally(craft);
        this.searchCache.clear();
      })
    );
  }

  getAllCrafts(): Observable<CraftItem[]> {
    return this.http.get<CraftItem[]>(this.apiUrl).pipe(
      tap((crafts) => {
        crafts.forEach((c) => this.indexCraftLocally(c));
      })
    );
  }

  getMyCrafts(): Observable<CraftItem[]> {
    return this.http.get<CraftItem[]>(`${this.apiUrl}/my`);
  }

  getCraftById(id: number): Observable<CraftItem> {
    return this.http.get<CraftItem>(`${this.apiUrl}/${id}`);
  }

  getCraftsByCategory(categoryId: number): Observable<CraftItem[]> {
    return this.http.get<CraftItem[]>(`${this.apiUrl}/category/${categoryId}`);
  }

  /**
   * Search crafts with LRU caching
   */
  searchCrafts(params: {
    keyword?: string;
    categoryId?: number;
    minPrice?: number;
    maxPrice?: number;
  }): Observable<CraftItem[]> {
    const cacheKey = JSON.stringify(params);
    const cached = this.searchCache.get(cacheKey);
    if (cached) {
      return of(cached);
    }

    let httpParams = new HttpParams();
    if (params.keyword) httpParams = httpParams.set('keyword', params.keyword);
    if (params.categoryId) httpParams = httpParams.set('categoryId', params.categoryId.toString());
    if (params.minPrice) httpParams = httpParams.set('minPrice', params.minPrice.toString());
    if (params.maxPrice) httpParams = httpParams.set('maxPrice', params.maxPrice.toString());

    return this.http.get<CraftItem[]>(`${this.apiUrl}/search`, { params: httpParams }).pipe(
      tap((data) => {
        this.searchCache.put(cacheKey, data);
        data.forEach((c) => this.indexCraftLocally(c));
      })
    );
  }

  /**
   * Trie Autocomplete from backend with fallback to local client Trie
   */
  getAutocompleteSuggestions(query: string, limit = 8): Observable<AutocompleteItem[]> {
    if (!query || !query.trim()) {
      return of([]);
    }

    const trimmed = query.trim().toLowerCase();
    // Fast path: Check local Trie first
    const localMatches = this.localSearchTrie.searchPrefix(trimmed, limit);
    if (localMatches.length > 0) {
      const items: AutocompleteItem[] = localMatches.map((m) => ({
        suggestion: m.word,
        matchCount: m.values.length,
      }));
      return of(items);
    }

    let params = new HttpParams().set('q', trimmed).set('limit', limit.toString());
    return this.http.get<AutocompleteItem[]>(`${this.apiUrl}/autocomplete`, { params });
  }

  private indexCraftLocally(craft: CraftItem): void {
    if (!craft || !craft.title) return;
    this.localSearchTrie.insert(craft.title, craft);
    craft.title.split(/\s+/).forEach((w) => {
      if (w.length >= 2) this.localSearchTrie.insert(w, craft);
    });
    if (craft.category?.name) {
      this.localSearchTrie.insert(craft.category.name, craft);
    }
  }

  updateCraft(id: number, craft: Partial<CraftItem>): Observable<CraftItem> {
    return this.http.put<CraftItem>(`${this.apiUrl}/${id}`, craft).pipe(
      tap(() => this.searchCache.clear())
    );
  }

  toggleLiveStatus(id: number, isLive?: boolean): Observable<CraftItem> {
    let params = new HttpParams();
    if (isLive !== undefined) {
      params = params.set('isLive', isLive.toString());
    }
    return this.http.patch<CraftItem>(`${this.apiUrl}/${id}/toggle-status`, {}, { params }).pipe(
      tap(() => this.searchCache.clear())
    );
  }

  deleteCraft(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${id}`, {
      responseType: 'text',
    }).pipe(
      tap(() => this.searchCache.clear())
    );
  }
}
