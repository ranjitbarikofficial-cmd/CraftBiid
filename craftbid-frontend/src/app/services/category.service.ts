import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { getApiBaseUrl } from './api-config';

export interface CategoryItem {
  id: number;
  name: string;
  description: string;
  imageUrl?: string;
}

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private apiUrl = `${getApiBaseUrl()}/api/categories`;
  private categories$?: Observable<CategoryItem[]>;

  constructor(private http: HttpClient) {}

  getAllCategories(): Observable<CategoryItem[]> {
    if (!this.categories$) {
      this.categories$ = this.http.get<CategoryItem[]>(this.apiUrl).pipe(
        shareReplay(1)
      );
    }
    return this.categories$;
  }

  getCategoryById(id: number): Observable<CategoryItem> {
    return this.http.get<CategoryItem>(`${this.apiUrl}/${id}`);
  }

  clearCache(): void {
    this.categories$ = undefined;
  }
}
