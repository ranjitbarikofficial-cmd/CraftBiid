import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AdminService, AdminStats, AdminUser } from '../../services/admin.service';
import { CraftService, CraftItem } from '../../services/craft.service';
import { AuctionService, AuctionItem } from '../../services/auction.service';
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
  users: AdminUser[] = [];
  crafts: CraftItem[] = [];
  auctions: AuctionItem[] = [];

  activeTab: 'overview' | 'users' | 'crafts' | 'auctions' = 'overview';
  loading = true;

  constructor(
    private adminService: AdminService,
    private craftService: CraftService,
    private auctionService: AuctionService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadUsers();
    this.loadCrafts();
    this.loadAuctions();
  }

  loadStats(): void {
    this.adminService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load admin stats:', err);
        this.loading = false;
      },
    });
  }

  loadUsers(): void {
    this.adminService.getAllUsers().subscribe({
      next: (data) => (this.users = data),
      error: (err) => console.error('Failed to load users:', err),
    });
  }

  loadCrafts(): void {
    this.adminService.getAllCrafts().subscribe({
      next: (data) => (this.crafts = data),
      error: (err) => console.error('Failed to load crafts:', err),
    });
  }

  loadAuctions(): void {
    this.adminService.getAllAuctions().subscribe({
      next: (data) => (this.auctions = data),
      error: (err) => console.error('Failed to load auctions:', err),
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
