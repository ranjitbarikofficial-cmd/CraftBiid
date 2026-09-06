import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CraftService, CraftItem } from '../../services/craft.service';
import { CraftReelService, CraftReelItem } from '../../services/craft-reel.service';
import { AuctionService, AuctionOrderItem } from '../../services/auction.service';
import { FollowService } from '../../services/follow.service';
import { AuthService, UserAuth } from '../../services/auth';
import { ToastService } from '../../services/toast.service';
import { resolveMediaUrl } from '../../services/api-config';

@Component({
  selector: 'app-artisan-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './artisan-dashboard.html',
  styleUrl: './artisan-dashboard.css',
})
export class ArtisanDashboard implements OnInit {
  resolveMediaUrl = resolveMediaUrl;
  currentUser: UserAuth | null = null;
  crafts: CraftItem[] = [];
  reels: CraftReelItem[] = [];
  orders: AuctionOrderItem[] = [];

  totalCrafts = 0;
  totalReels = 0;
  totalViews = 0;
  totalLikes = 0;
  totalRevenue = 0;
  totalFollowers = 0;

  activeTab: 'crafts' | 'reels' | 'orders' = 'crafts';
  loading = false;
  errorMessage = '';

  // Auction modal
  isAuctionModalOpen = false;
  selectedCraft: CraftItem | null = null;
  auctionStartingPrice = 0;
  auctionDurationHours = 24;
  auctionMinIncrement = 50;
  auctionLoading = false;
  auctionError = '';
  auctionSuccess = '';

  constructor(
    private craftService: CraftService,
    private craftReelService: CraftReelService,
    private auctionService: AuctionService,
    private followService: FollowService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;
    this.errorMessage = '';

    if (this.currentUser?.userId) {
      this.followService.getFollowerCount(this.currentUser.userId).subscribe({
        next: (res) => {
          this.totalFollowers = res.followerCount || 0;
        },
      });
    }

    this.craftService.getMyCrafts().subscribe({
      next: (crafts) => {
        this.crafts = crafts;
        this.totalCrafts = crafts.length;
        this.loadReelsData();
        this.loadOrdersData();
      },
      error: (err) => {
        console.error('Failed to load artisan crafts:', err);
        this.loading = false;
        this.errorMessage = 'Unable to load your studio creations.';
      },
    });
  }

  loadReelsData(): void {
    this.craftReelService.getMyReels().subscribe({
      next: (reels) => {
        this.reels = reels;
        this.totalReels = reels.length;
        this.totalViews = reels.reduce((sum, r) => sum + (r.views || 0), 0);
        this.totalLikes = reels.reduce((sum, r) => sum + (r.likes || 0), 0);
      },
      error: (err) => {
        console.error('Failed to load artisan reels:', err);
      },
    });
  }

  loadOrdersData(): void {
    this.auctionService.getArtisanOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.totalRevenue = orders.reduce((sum, o) => sum + Number(o.artisanPayout || 0), 0);
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load artisan orders:', err);
        this.loading = false;
      },
    });
  }

  openAuctionModal(craft: CraftItem, event: Event): void {
    event.stopPropagation();
    this.selectedCraft = craft;
    this.auctionStartingPrice = craft.basePrice;
    this.auctionDurationHours = 24;
    this.auctionMinIncrement = 50;
    this.auctionError = '';
    this.auctionSuccess = '';
    this.isAuctionModalOpen = true;
  }

  closeAuctionModal(): void {
    this.isAuctionModalOpen = false;
    this.selectedCraft = null;
  }

  submitAuction(): void {
    if (!this.selectedCraft) return;

    if (!this.auctionStartingPrice || this.auctionStartingPrice <= 0) {
      this.auctionError = 'Starting price must be greater than zero.';
      return;
    }

    this.auctionLoading = true;
    this.auctionError = '';
    this.auctionSuccess = '';

    this.auctionService
      .createAuction({
        craftId: this.selectedCraft.id,
        startingPrice: this.auctionStartingPrice,
        minBidIncrement: this.auctionMinIncrement,
        durationHours: this.auctionDurationHours,
      })
      .subscribe({
        next: (auction) => {
          this.auctionLoading = false;
          this.auctionSuccess = '🎉 Live auction room launched successfully!';
          this.toastService.success('🚀 Auction room opened! Turn timer active.');
          setTimeout(() => {
            this.closeAuctionModal();
            this.router.navigate(['/auctions', auction.id]);
          }, 1200);
        },
        error: (err) => {
          this.auctionLoading = false;
          this.auctionError =
            err.error?.message ||
            err.error ||
            (typeof err.error === 'string' ? err.error : 'Failed to launch auction.');
          this.toastService.error(this.auctionError);
        },
      });
  }

  toggleCraftStatus(craft: CraftItem, event: Event): void {
    event.stopPropagation();
    const newStatus = craft.status === 'ACTIVE' ? 'OFFLINE' : 'ACTIVE';
    const isLive = newStatus === 'ACTIVE';

    this.craftService.toggleLiveStatus(craft.id, isLive).subscribe({
      next: (updated) => {
        craft.status = updated.status;
        if (isLive) {
          this.toastService.success(`🟢 "${craft.title}" is now LIVE for auctions and collectors!`);
        } else {
          this.toastService.info(`🔴 "${craft.title}" is taken OFFLINE.`);
        }
      },
      error: (err) => {
        console.error('Failed to toggle craft status:', err);
        this.toastService.error('Failed to update craft live status.');
      },
    });
  }

  deleteCraft(id: number, event: Event): void {
    event.stopPropagation();
    if (!confirm('Are you sure you want to delete this craft?')) {
      return;
    }

    this.craftService.deleteCraft(id).subscribe({
      next: () => {
        this.crafts = this.crafts.filter((c) => c.id !== id);
        this.totalCrafts = this.crafts.length;
        this.toastService.success('Craft deleted from your studio.');
      },
      error: (err) => {
        console.error('Failed to delete craft:', err);
        this.toastService.error('Failed to delete craft.');
      },
    });
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400&q=80';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
