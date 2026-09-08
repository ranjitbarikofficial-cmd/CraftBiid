import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { AuthService } from './auth';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: 'AUCTION' | 'BID' | 'REFUND' | 'ORDER' | 'REEL' | 'SYSTEM';
  time: string;
  read: boolean;
  link?: string;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private currentUserId: string = 'guest';
  private notificationsSubject = new BehaviorSubject<NotificationItem[]>([]);
  public notifications$: Observable<NotificationItem[]> = this.notificationsSubject.asObservable();

  constructor(private authService: AuthService) {
    // Purge legacy mock notifications if stored under the old generic key
    try {
      const oldData = localStorage.getItem('craftbid_notifications');
      if (oldData && oldData.includes('notif-1')) {
        localStorage.removeItem('craftbid_notifications');
      }
    } catch (_) {}

    // Subscribe to user changes to load user-specific notifications
    this.authService.currentUser$.subscribe((user) => {
      this.currentUserId = user?.userId ? String(user.userId) : (user?.email || 'guest');
      this.loadUserNotifications();
    });
  }

  private getStorageKey(): string {
    return `craftbid_notifs_${this.currentUserId}`;
  }

  private loadUserNotifications(): void {
    try {
      const raw = localStorage.getItem(this.getStorageKey());
      if (raw) {
        const parsed: NotificationItem[] = JSON.parse(raw);
        // Filter out any stale mock notif-1/2/3 data
        const clean = parsed.filter((n) => !n.id.startsWith('notif-'));
        if (clean.length !== parsed.length) {
          this.saveToStorage(clean);
          return;
        }
        this.notificationsSubject.next(clean);
        return;
      }
    } catch (e) {
      console.error('Failed to load notifications from storage:', e);
    }

    // Default for newly registered or first-time users: A clean, genuine welcome message
    const defaultWelcome: NotificationItem[] = [
      {
        id: 'welcome-' + Date.now(),
        title: '✨ Welcome to CraftBid!',
        message:
          "Explore India's 1st live turn craft marketplace. Join live auctions, watch artisan reels, and connect with master artisans.",
        type: 'SYSTEM',
        time: 'Just now',
        read: false,
        link: '/auctions',
      },
    ];

    this.saveToStorage(defaultWelcome);
  }

  private saveToStorage(notifications: NotificationItem[]): void {
    try {
      localStorage.setItem(this.getStorageKey(), JSON.stringify(notifications));
      this.notificationsSubject.next(notifications);
    } catch (e) {
      console.error('Failed to save notifications to storage:', e);
    }
  }

  getNotifications(): NotificationItem[] {
    return this.notificationsSubject.getValue();
  }

  getUnreadCount(): number {
    return this.notificationsSubject.getValue().filter((n) => !n.read).length;
  }

  markAsRead(id: string): void {
    const list = this.getNotifications().map((n) => (n.id === id ? { ...n, read: true } : n));
    this.saveToStorage(list);
  }

  markAllAsRead(): void {
    const list = this.getNotifications().map((n) => ({ ...n, read: true }));
    this.saveToStorage(list);
  }

  clearAll(): void {
    this.saveToStorage([]);
  }

  addNotification(notif: Omit<NotificationItem, 'id' | 'time' | 'read'>): void {
    const newNotif: NotificationItem = {
      ...notif,
      id: 'notif-' + Date.now(),
      time: 'Just now',
      read: false,
    };
    const list = [newNotif, ...this.getNotifications()];
    this.saveToStorage(list);
  }
}
