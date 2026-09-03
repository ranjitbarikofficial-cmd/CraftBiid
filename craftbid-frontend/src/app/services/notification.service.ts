import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

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
  private storageKey = 'craftbid_notifications';
  private notificationsSubject: BehaviorSubject<NotificationItem[]>;
  public notifications$: Observable<NotificationItem[]>;

  constructor() {
    const initial = this.loadFromStorage();
    this.notificationsSubject = new BehaviorSubject<NotificationItem[]>(initial);
    this.notifications$ = this.notificationsSubject.asObservable();
  }

  private loadFromStorage(): NotificationItem[] {
    try {
      const data = localStorage.getItem(this.storageKey);
      if (data) {
        return JSON.parse(data);
      }
    } catch (e) {
      console.error('Failed to load notifications from storage:', e);
    }

    // Default notifications for real startup experience
    return [
      {
        id: 'notif-1',
        title: '🏆 You Won Auction #1!',
        message: 'Congratulations! Your highest bid won "Handcrafted Clay Vase". Please confirm shipping address.',
        type: 'AUCTION',
        time: 'Just now',
        read: false,
        link: '/auctions/1',
      },
      {
        id: 'notif-2',
        title: '💳 100% Refund Processed',
        message: 'Automated refund of ₹1,600 has been credited to your UPI account for non-winning auction turn.',
        type: 'REFUND',
        time: '10 mins ago',
        read: false,
        link: '/profile',
      },
      {
        id: 'notif-3',
        title: '📦 New Dispatch Order Received',
        message: 'Buyer Ranjit Barik submitted delivery address for Order #CB-ORD-1. Net payout: ₹1,620.',
        type: 'ORDER',
        time: '25 mins ago',
        read: false,
        link: '/artisan-dashboard',
      },
      {
        id: 'notif-4',
        title: '🎥 New Craft Reel Published',
        message: 'Artisan Rajesh Sharma uploaded a new process reel: "Sculpting Terracotta Clay".',
        type: 'REEL',
        time: '1 hour ago',
        read: true,
        link: '/reels',
      },
      {
        id: 'notif-5',
        title: '⚡ 1-Minute Live Turn Alert',
        message: 'A new live turn auction for "Wood Carved Elephant" has opened with 10 collector seats.',
        type: 'SYSTEM',
        time: '3 hours ago',
        read: true,
        link: '/auctions',
      },
    ];
  }

  private saveToStorage(notifications: NotificationItem[]): void {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(notifications));
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
