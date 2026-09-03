import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserAuth } from '../../services/auth';
import { PaymentService, PaymentTransactionItem } from '../../services/payment.service';
import { FollowService, ArtisanProfile } from '../../services/follow.service';
import { ToastService } from '../../services/toast.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  currentUser: UserAuth | null = null;
  transactions: PaymentTransactionItem[] = [];
  followedArtisans: ArtisanProfile[] = [];
  loadingTransactions = false;
  loadingFollows = false;

  totalSpent = 0;
  totalRefunded = 0;

  activeTab: 'wallet' | 'following' = 'wallet';

  constructor(
    private authService: AuthService,
    private paymentService: PaymentService,
    private followService: FollowService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.currentUser = this.authService.getCurrentUser();
    this.loadTransactions();
    this.loadFollowedArtisans();
  }

  setTab(tab: 'wallet' | 'following'): void {
    this.activeTab = tab;
  }

  loadTransactions(): void {
    this.loadingTransactions = true;
    this.paymentService.getMyTransactions().subscribe({
      next: (txs) => {
        this.transactions = txs;
        this.loadingTransactions = false;

        this.totalSpent = txs
          .filter((t) => t.type !== 'AUTO_REFUND' && t.status === 'SUCCESS')
          .reduce((sum, t) => sum + Number(t.amount), 0);

        this.totalRefunded = txs
          .filter((t) => t.type === 'AUTO_REFUND' || t.status === 'REFUNDED')
          .reduce((sum, t) => sum + Number(t.amount), 0);
      },
      error: (err) => {
        console.error('Failed to load transactions:', err);
        this.loadingTransactions = false;
      },
    });
  }

  loadFollowedArtisans(): void {
    this.loadingFollows = true;
    this.followService.getMyFollowedArtisans().subscribe({
      next: (artisans) => {
        this.followedArtisans = artisans;
        this.loadingFollows = false;
      },
      error: (err) => {
        console.error('Failed to load followed artisans:', err);
        this.loadingFollows = false;
      },
    });
  }

  unfollowArtisan(artisanUserId: number, event: Event): void {
    event.stopPropagation();
    this.followService.toggleFollow(artisanUserId).subscribe({
      next: (res) => {
        this.toastService.info(res.message || 'Unfollowed artisan');
        this.loadFollowedArtisans();
      },
      error: (err) => {
        this.toastService.error('Could not unfollow artisan');
      },
    });
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString([], {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
