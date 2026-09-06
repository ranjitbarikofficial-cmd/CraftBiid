import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth';
import { getApiBaseUrl } from './api-config';

export interface FollowStatusResponse {
  following: boolean;
  followerCount: number;
  artisanId: number;
  message?: string;
}

export interface ArtisanProfile {
  id: number;
  shopName: string;
  craftType: string;
  city: string;
  user: {
    id: number;
    name: string;
    email: string;
    phone?: string;
  };
}

@Injectable({
  providedIn: 'root',
})
export class FollowService {
  private apiUrl = `${getApiBaseUrl()}/api/follow`;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
  ) {}

  private getAuthHeaders() {
    return {
      headers: {
        Authorization: `Bearer ${this.authService.getToken() || ''}`,
      },
    };
  }

  toggleFollow(artisanUserId: number): Observable<FollowStatusResponse> {
    return this.http.post<FollowStatusResponse>(
      `${this.apiUrl}/toggle/${artisanUserId}`,
      {},
      this.getAuthHeaders(),
    );
  }

  getFollowStatus(artisanUserId: number): Observable<FollowStatusResponse> {
    const token = this.authService.getToken();
    if (token) {
      return this.http.get<FollowStatusResponse>(
        `${this.apiUrl}/status/${artisanUserId}`,
        this.getAuthHeaders(),
      );
    }
    return this.http.get<FollowStatusResponse>(`${this.apiUrl}/count/${artisanUserId}`);
  }

  getFollowerCount(
    artisanUserId: number,
  ): Observable<{ artisanId: number; followerCount: number }> {
    return this.http.get<{ artisanId: number; followerCount: number }>(
      `${this.apiUrl}/count/${artisanUserId}`,
    );
  }

  getMyFollowedArtisans(): Observable<ArtisanProfile[]> {
    return this.http.get<ArtisanProfile[]>(`${this.apiUrl}/my-artisans`, this.getAuthHeaders());
  }

  getFollowingReels(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/following-reels`, this.getAuthHeaders());
  }

  getFollowingCrafts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/following-crafts`, this.getAuthHeaders());
  }
}
