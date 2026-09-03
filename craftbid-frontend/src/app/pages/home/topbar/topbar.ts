import { Component, OnInit, HostListener, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserAuth } from '../../../services/auth';
import { NotificationService, NotificationItem } from '../../../services/notification.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
})
export class Topbar implements OnInit {
  searchQuery = '';
  currentUser: UserAuth | null = null;
  isLoggedIn = false;

  // Notifications state
  notifications: NotificationItem[] = [];
  unreadCount = 0;
  isNotifOpen = false;

  // Settings modal state
  isSettingsOpen = false;
  activeSettingsTab: 'profile' | 'notifications' | 'payouts' = 'profile';

  settingsForm = {
    displayName: '',
    phone: '',
    workshopCity: 'Bhubaneswar, Odisha',
    bio: 'Traditional handcrafted artist preserving Indian cultural heritage.',
    notifyOutbid: true,
    notifyTurnTimer: true,
    notifyReels: false,
    upiId: 'artisan@okaxis',
    bankAccount: '918237491823',
    ifscCode: 'HDFC0001234',
  };

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private router: Router,
    private elRef: ElementRef
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
      this.isLoggedIn = this.authService.isLoggedIn();
      if (user) {
        this.settingsForm.displayName = user.name || '';
        this.settingsForm.phone = user.phone || '7077476718';
      }
    });

    this.notificationService.notifications$.subscribe((notifs) => {
      this.notifications = notifs;
      this.unreadCount = this.notificationService.getUnreadCount();
    });
  }

  // Close notifications on outside click
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!this.elRef.nativeElement.querySelector('.notif-container')?.contains(target)) {
      this.isNotifOpen = false;
    }
  }

  toggleNotifications(event: Event): void {
    event.stopPropagation();
    this.isNotifOpen = !this.isNotifOpen;
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead();
    this.toastService.success('All notifications marked as read');
  }

  clearAllNotifs(): void {
    this.notificationService.clearAll();
    this.toastService.success('Notification feed cleared');
  }

  openNotification(notif: NotificationItem): void {
    this.notificationService.markAsRead(notif.id);
    this.isNotifOpen = false;
    if (notif.link) {
      this.router.navigateByUrl(notif.link);
    }
  }

  openSettings(): void {
    this.isSettingsOpen = true;
    this.isNotifOpen = false;
  }

  closeSettings(): void {
    this.isSettingsOpen = false;
  }

  saveSettings(): void {
    this.toastService.success('⚙️ Studio & Account settings saved successfully!');
    this.closeSettings();
  }

  onSearch(): void {
    if (!this.searchQuery.trim()) {
      return;
    }
    this.router.navigate(['/search'], {
      queryParams: { q: this.searchQuery.trim() },
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
