import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiBaseUrl } from './api-config';

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  type: string;
  read: boolean;
  link?: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private apiUrl = `${getApiBaseUrl()}/api/notifications`;

  constructor(private http: HttpClient) {}

  getMyNotifications(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(this.apiUrl);
  }

  getUnreadCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.apiUrl}/unread-count`);
  }

  markAsRead(id: number): Observable<NotificationItem> {
    return this.http.patch<NotificationItem>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/mark-all-read`, {});
  }
}
