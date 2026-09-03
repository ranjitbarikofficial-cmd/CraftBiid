import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

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

@Injectable({
  providedIn: 'root',
})
export class CraftService {
  private apiUrl = '/api/crafts';

  constructor(private http: HttpClient) {}

  uploadCraft(formData: FormData): Observable<CraftItem> {
    return this.http.post<CraftItem>(`${this.apiUrl}/upload`, formData);
  }

  getAllCrafts(): Observable<CraftItem[]> {
    return this.http.get<CraftItem[]>(this.apiUrl);
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

  searchCrafts(params: {
    keyword?: string;
    categoryId?: number;
    minPrice?: number;
    maxPrice?: number;
  }): Observable<CraftItem[]> {
    let httpParams = new HttpParams();
    if (params.keyword) httpParams = httpParams.set('keyword', params.keyword);
    if (params.categoryId) httpParams = httpParams.set('categoryId', params.categoryId.toString());
    if (params.minPrice) httpParams = httpParams.set('minPrice', params.minPrice.toString());
    if (params.maxPrice) httpParams = httpParams.set('maxPrice', params.maxPrice.toString());

    return this.http.get<CraftItem[]>(`${this.apiUrl}/search`, { params: httpParams });
  }

  updateCraft(id: number, craft: Partial<CraftItem>): Observable<CraftItem> {
    return this.http.put<CraftItem>(`${this.apiUrl}/${id}`, craft);
  }

  toggleLiveStatus(id: number, isLive?: boolean): Observable<CraftItem> {
    let params = new HttpParams();
    if (isLive !== undefined) {
      params = params.set('isLive', isLive.toString());
    }
    return this.http.patch<CraftItem>(`${this.apiUrl}/${id}/toggle-status`, {}, { params });
  }

  deleteCraft(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${id}`, {
      responseType: 'text',
    });
  }
}
