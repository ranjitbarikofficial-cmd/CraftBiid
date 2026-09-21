import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminService, AdminStats, AdminUser } from '../../services/admin.service';
import { CraftService, CraftItem } from '../../services/craft.service';
import { AuctionService, AuctionItem } from '../../services/auction.service';
import { PaymentService, PaymentStats, PaymentTransactionItem } from '../../services/payment.service';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboard implements OnInit {
  stats: AdminStats | null = null;
  paymentStats: PaymentStats | null = null;
  users: AdminUser[] = [];
  crafts: CraftItem[] = [];
  auctions: AuctionItem[] = [];

  activeTab: 'overview' | 'users' | 'crafts' | 'auctions' = 'overview';
  
  overviewLoading = true;
  usersLoading = true;
  craftsLoading = true;
  auctionsLoading = true;
  loading = false;

  constructor(
    private adminService: AdminService,
    private craftService: CraftService,
    private auctionService: AuctionService,
    private paymentService: PaymentService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadPaymentStats();
    this.loadUsers();
    this.loadCrafts();
    this.loadAuctions();
  }

  loadPaymentStats(): void {
    this.paymentService.getAdminPaymentStats().subscribe({
      next: (data) => {
        this.paymentStats = data;
        this.cdr.markForCheck();
      },
      error: (err) => console.error('Failed to load payment stats:', err),
    });
  }

  loadStats(): void {
    this.overviewLoading = true;
    this.cdr.markForCheck();

    this.adminService
      .getStats()
      .pipe(
        finalize(() => {
          this.overviewLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (data) => {
          this.stats = data;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load admin stats:', err);
          this.cdr.markForCheck();
        },
      });
  }

  loadUsers(): void {
    this.usersLoading = true;
    this.cdr.markForCheck();

    this.adminService
      .getAllUsers()
      .pipe(
        finalize(() => {
          this.usersLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (data) => {
          this.users = data || [];
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load users:', err);
          this.cdr.markForCheck();
        },
      });
  }

  loadCrafts(): void {
    this.craftsLoading = true;
    this.cdr.markForCheck();

    this.adminService
      .getAllCrafts()
      .pipe(
        finalize(() => {
          this.craftsLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (data) => {
          this.crafts = data || [];
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load crafts:', err);
          this.cdr.markForCheck();
        },
      });
  }

  loadAuctions(): void {
    this.auctionsLoading = true;
    this.cdr.markForCheck();

    this.adminService
      .getAllAuctions()
      .pipe(
        finalize(() => {
          this.auctionsLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (data) => {
          this.auctions = data || [];
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load auctions:', err);
          this.cdr.markForCheck();
        },
      });
  }

  toggleUser(user: AdminUser): void {
    this.adminService.toggleUserStatus(user.id).subscribe({
      next: (updated) => {
        user.active = updated.active;
      },
      error: (err) => alert('Failed to update user status.'),
    });
  }

  toggleCraftStatus(craft: CraftItem): void {
    const isLive = craft.status !== 'ACTIVE';
    this.craftService.toggleLiveStatus(craft.id, isLive).subscribe({
      next: (updated) => {
        craft.status = updated.status;
      },
      error: (err) => alert('Failed to update craft live status.'),
    });
  }

  deleteCraft(id: number): void {
    if (!confirm('Are you sure you want to delete this craft as admin?')) return;
    this.craftService.deleteCraft(id).subscribe({
      next: () => {
        this.crafts = this.crafts.filter((c) => c.id !== id);
        this.loadStats();
      },
      error: (err) => alert('Failed to delete craft.'),
    });
  }

  cancelAuction(id: number): void {
    if (!confirm('Are you sure you want to cancel this auction?')) return;
    this.auctionService.cancelAuction(id).subscribe({
      next: (updated) => {
        const item = this.auctions.find((a) => a.id === id);
        if (item) item.status = 'CANCELLED';
        this.loadStats();
      },
      error: (err) => alert('Failed to cancel auction.'),
    });
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
