import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { getApiBaseUrl } from './api-config';
import { AuthService } from './auth';

export interface ArtisanProfile {
  id: number;
  userId: number;
  name: string;
  email: string;
  phone?: string;
  city: string;
  shopName: string;
  craftType: string;
  profileImageUrl?: string;
  createdAt: string;
}

export interface UpdateArtisanProfileRequest {
  name?: string;
  shopName?: string;
  craftType?: string;
  city?: string;
  phone?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ArtisanService {
  private apiUrl = `${getApiBaseUrl()}/api/artisan`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getProfile(): Observable<ArtisanProfile> {
    return this.http.get<ArtisanProfile>(`${this.apiUrl}/profile`).pipe(
      tap((profile) => {
        if (profile) {
          this.authService.updateCurrentUser({
            name: profile.name,
            phone: profile.phone,
            profileImageUrl: profile.profileImageUrl,
          });
        }
      })
    );
  }

  updateProfile(data: UpdateArtisanProfileRequest): Observable<ArtisanProfile> {
    return this.http.put<ArtisanProfile>(`${this.apiUrl}/profile`, data).pipe(
      tap((profile) => {
        if (profile) {
          this.authService.updateCurrentUser({
            name: profile.name,
            phone: profile.phone,
            profileImageUrl: profile.profileImageUrl,
          });
        }
      })
    );
  }

  uploadProfilePhoto(file: File): Observable<ArtisanProfile> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ArtisanProfile>(`${this.apiUrl}/profile-photo`, formData).pipe(
      tap((profile) => {
        if (profile) {
          this.authService.updateCurrentUser({
            profileImageUrl: profile.profileImageUrl,
          });
        }
      })
    );
  }

  enableArtisan(data: { shopName: string; craftType: string; city: string }): Observable<string> {
    return this.http.post(`${this.apiUrl}/enable`, data, {
      responseType: 'text',
    }).pipe(
      tap(() => {
        this.authService.setSellerEnabled(true);
      })
    );
  }
}
