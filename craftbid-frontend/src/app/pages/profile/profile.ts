import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserAuth } from '../../services/auth';
import { PaymentService, PaymentTransactionItem } from '../../services/payment.service';
import { AuctionService, AuctionOrderItem } from '../../services/auction.service';
import { FollowService, ArtisanProfile } from '../../services/follow.service';
import { ToastService } from '../../services/toast.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';
import { resolveMediaUrl } from '../../services/api-config';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  resolveMediaUrl = resolveMediaUrl;
  currentUser: UserAuth | null = null;
  transactions: PaymentTransactionItem[] = [];
  refunds: PaymentTransactionItem[] = [];
  orders: AuctionOrderItem[] = [];
  followedArtisans: ArtisanProfile[] = [];

  loadingTransactions = false;
  loadingRefunds = false;
  loadingOrders = false;
  loadingFollows = false;

  totalSpent = 0;
  totalRefunded = 0;

  activeTab: 'orders' | 'refunds' | 'wallet' | 'following' = 'orders';

  constructor(
    private authService: AuthService,
    private paymentService: PaymentService,
    private auctionService: AuctionService,
    private followService: FollowService,
    private toastService: ToastService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.currentUser = this.authService.getCurrentUser();
    this.loadOrders();
    this.loadRefunds();
    this.loadTransactions();
    this.loadFollowedArtisans();
  }

  setTab(tab: 'orders' | 'refunds' | 'wallet' | 'following'): void {
    this.activeTab = tab;
  }

  loadOrders(): void {
    this.loadingOrders = true;
    this.auctionService.getBuyerOrders().subscribe({
      next: (data) => {
        this.orders = data;
        this.loadingOrders = false;
      },
      error: () => {
        this.loadingOrders = false;
      },
    });
  }

  loadRefunds(): void {
    this.loadingRefunds = true;
    this.paymentService.getMyRefunds().subscribe({
      next: (data) => {
        this.refunds = data;
        this.loadingRefunds = false;
      },
      error: () => {
        this.loadingRefunds = false;
      },
    });
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
      error: () => {
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
      error: () => {
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
      error: () => {
        this.toastService.error('Could not unfollow artisan');
      },
    });
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
