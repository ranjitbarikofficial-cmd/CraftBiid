import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CraftItem } from './craft.service';
import { AuctionItem } from './auction.service';

export interface AdminStats {
  totalUsers: number;
  totalArtisans: number;
  totalCrafts: number;
  totalAuctions: number;
  totalActiveAuctions: number;
  totalBids: number;
}

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  phone?: string;
  role: string;
  active: boolean;
  sellerEnabled: boolean;
  createdAt?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private apiUrl = '/api/admin';

  constructor(private http: HttpClient) {}

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.apiUrl}/stats`);
  }

  getAllUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.apiUrl}/users`);
  }

  toggleUserStatus(userId: number): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.apiUrl}/users/${userId}/toggle-status`, {});
  }

  getAllCrafts(): Observable<CraftItem[]> {
    return this.http.get<CraftItem[]>(`${this.apiUrl}/crafts`);
  }

  getAllAuctions(): Observable<AuctionItem[]> {
    return this.http.get<AuctionItem[]>(`${this.apiUrl}/auctions`);
  }
}
