import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CraftReelItem {
  id: number;
  title: string;
  description: string;
  videoUrl: string;
  thumbnailUrl?: string;
  views: number;
  likes: number;
  status: string;
  createdAt: string;
  artisan?: {
    id: number;
    shopName: string;
    craftType: string;
    city: string;
    user?: {
      id: number;
      name: string;
      email: string;
    };
  };
  craft?: {
    id: number;
    title: string;
    basePrice: number;
    imageUrl: string;
  };
}

@Injectable({
  providedIn: 'root',
})
export class CraftReelService {
  private apiUrl = '/api/craft-reels';

  constructor(private http: HttpClient) {}

  getHomeReels(): Observable<CraftReelItem[]> {
    return this.http.get<CraftReelItem[]>(`${this.apiUrl}/home`);
  }

  getMyReels(): Observable<CraftReelItem[]> {
    return this.http.get<CraftReelItem[]>(`${this.apiUrl}/my`);
  }

  getReelsByCraftId(craftId: number): Observable<CraftReelItem[]> {
    return this.http.get<CraftReelItem[]>(`${this.apiUrl}/craft/${craftId}`);
  }

  incrementViews(id: number): Observable<CraftReelItem> {
    return this.http.post<CraftReelItem>(`${this.apiUrl}/${id}/view`, {});
  }

  likeReel(id: number): Observable<CraftReelItem> {
    return this.http.post<CraftReelItem>(`${this.apiUrl}/${id}/like`, {});
  }

  createReel(data: {
    craftId: number;
    title: string;
    description?: string;
    videoUrl: string;
    thumbnailUrl?: string;
  }): Observable<CraftReelItem> {
    return this.http.post<CraftReelItem>(this.apiUrl, null, {
      params: {
        craftId: data.craftId.toString(),
        title: data.title,
        description: data.description || '',
        videoUrl: data.videoUrl,
        thumbnailUrl: data.thumbnailUrl || '',
      },
    });
  }
}
