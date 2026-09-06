import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  constructor(private http: HttpClient) {}

  getAllCategories(): Observable<CategoryItem[]> {
    return this.http.get<CategoryItem[]>(this.apiUrl);
  }

  getCategoryById(id: number): Observable<CategoryItem> {
    return this.http.get<CategoryItem>(`${this.apiUrl}/${id}`);
  }
}
