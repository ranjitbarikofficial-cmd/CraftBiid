import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CraftService, CraftItem } from '../../services/craft.service';
import { CraftReelService, CraftReelItem } from '../../services/craft-reel.service';
import { AuctionService, AuctionOrderItem } from '../../services/auction.service';
import { FollowService } from '../../services/follow.service';
import { AuthService, UserAuth } from '../../services/auth';
import { ArtisanService, ArtisanProfile, UpdateArtisanProfileRequest } from '../../services/artisan.service';
import { CategoryService, CategoryItem } from '../../services/category.service';
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
  artisanProfile: ArtisanProfile | null = null;
  categories: CategoryItem[] = [];

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

  // Independent, section-specific loading states
  catalogLoading = true;
  reelsLoading = true;
  ordersLoading = true;
  loading = false;
  errorMessage = '';

  // 1. Profile Edit Modal State
  isProfileModalOpen = false;
  profileForm: UpdateArtisanProfileRequest = {
    name: '',
    shopName: '',
    craftType: '',
    city: '',
    phone: '',
  };
  selectedAvatarFile: File | null = null;
  avatarPreviewUrl: string | null = null;
  profileLoading = false;
  profileError = '';

  // 2. Craft Edit Modal State
  isEditCraftModalOpen = false;
  editingCraft: CraftItem | null = null;
  craftForm = {
    title: '',
    category: '',
    description: '',
    basePrice: 0,
    isLiveForAuction: true,
  };
  selectedCraftImageFile: File | null = null;
  craftImagePreviewUrl: string | null = null;
  craftEditLoading = false;
  craftEditError = '';

  // 3. Reel Edit Modal State
  isEditReelModalOpen = false;
  editingReel: CraftReelItem | null = null;
  reelForm = {
    title: '',
    description: '',
    videoUrl: '',
    thumbnailUrl: '',
  };
  reelEditLoading = false;
  reelEditError = '';

  // 4. Live Auction Modal State
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
    private artisanService: ArtisanService,
    private categoryService: CategoryService,
    private toastService: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadProfile();
    this.loadCategories();
    this.loadCrafts();
    this.loadReelsData();
    this.loadOrdersData();
    this.loadFollowerCount();
  }

  loadProfile(): void {
    this.artisanService.getProfile().subscribe({
      next: (profile) => {
        this.artisanProfile = profile;
        if (this.currentUser) {
          this.currentUser.name = profile.name;
          this.currentUser.profileImageUrl = profile.profileImageUrl;
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Could not load artisan profile info:', err);
      },
    });
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (cats) => {
        this.categories = cats;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Could not load categories:', err);
      },
    });
  }

  loadFollowerCount(): void {
    if (this.currentUser?.userId) {
      this.followService.getFollowerCount(this.currentUser.userId).subscribe({
        next: (res) => {
          this.totalFollowers = res.followerCount || 0;
          this.cdr.markForCheck();
        },
        error: () => {},
      });
    }
  }

  loadCrafts(): void {
    this.catalogLoading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.craftService
      .getMyCrafts()
      .pipe(
        finalize(() => {
          this.catalogLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (crafts) => {
          this.crafts = crafts || [];
          this.totalCrafts = this.crafts.length;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load artisan crafts:', err);
          this.errorMessage = 'Unable to load your studio creations.';
          this.cdr.markForCheck();
        },
      });
  }

  loadReelsData(): void {
    this.reelsLoading = true;
    this.cdr.markForCheck();

    this.craftReelService
      .getMyReels()
      .pipe(
        finalize(() => {
          this.reelsLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (reels) => {
          this.reels = reels || [];
          this.totalReels = this.reels.length;
          this.totalViews = this.reels.reduce((sum, r) => sum + (r.views || 0), 0);
          this.totalLikes = this.reels.reduce((sum, r) => sum + (r.likes || 0), 0);
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load artisan reels:', err);
          this.cdr.markForCheck();
        },
      });
  }

  loadOrdersData(): void {
    this.ordersLoading = true;
    this.cdr.markForCheck();

    this.auctionService
      .getArtisanOrders()
      .pipe(
        finalize(() => {
          this.ordersLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (orders) => {
          this.orders = orders || [];
          this.totalRevenue = this.orders.reduce((sum, o) => sum + Number(o.artisanPayout || 0), 0);
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load artisan orders:', err);
          this.cdr.markForCheck();
        },
      });
  }

  // ==========================================
  // 1. ARTISAN PROFILE & AVATAR ACTIONS
  // ==========================================

  openEditProfileModal(): void {
    this.profileForm = {
      name: this.artisanProfile?.name || this.currentUser?.name || '',
      shopName: this.artisanProfile?.shopName || '',
      craftType: this.artisanProfile?.craftType || '',
      city: this.artisanProfile?.city || '',
      phone: this.artisanProfile?.phone || this.currentUser?.phone || '',
    };
    this.selectedAvatarFile = null;
    this.avatarPreviewUrl = this.artisanProfile?.profileImageUrl
      ? resolveMediaUrl(this.artisanProfile.profileImageUrl)
      : null;
    this.profileError = '';
    this.isProfileModalOpen = true;
  }

  closeEditProfileModal(): void {
    this.isProfileModalOpen = false;
    this.selectedAvatarFile = null;
    this.avatarPreviewUrl = null;
    this.profileError = '';
  }

  onAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      if (file.size > 10 * 1024 * 1024) {
        this.profileError = 'Profile photo must be less than 10 MB.';
        return;
      }
      this.selectedAvatarFile = file;
      this.profileError = '';

      const reader = new FileReader();
      reader.onload = () => {
        this.avatarPreviewUrl = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  saveProfile(): void {
    if (!this.profileForm.name?.trim()) {
      this.profileError = 'Your name is required.';
      return;
    }

    this.profileLoading = true;
    this.profileError = '';

    // Step 1: Upload avatar if newly selected
    if (this.selectedAvatarFile) {
      this.artisanService.uploadProfilePhoto(this.selectedAvatarFile).subscribe({
        next: (updatedProfile) => {
          this.artisanProfile = updatedProfile;
          if (this.currentUser) {
            this.currentUser.profileImageUrl = updatedProfile.profileImageUrl;
          }
          this.saveProfileDetails();
        },
        error: (err) => {
          this.profileLoading = false;
          this.profileError = err.error?.message || 'Failed to upload profile photo.';
          this.toastService.error(this.profileError);
        },
      });
    } else {
      this.saveProfileDetails();
    }
  }

  private saveProfileDetails(): void {
    this.artisanService.updateProfile(this.profileForm).subscribe({
      next: (profile) => {
        this.artisanProfile = profile;
        if (this.currentUser) {
          this.currentUser.name = profile.name;
          this.currentUser.phone = profile.phone;
          this.currentUser.profileImageUrl = profile.profileImageUrl;
        }
        this.profileLoading = false;
        this.closeEditProfileModal();
        this.toastService.success('✨ Studio profile updated successfully!');
      },
      error: (err) => {
        this.profileLoading = false;
        this.profileError = err.error?.message || 'Failed to update profile details.';
        this.toastService.error(this.profileError);
      },
    });
  }

  // ==========================================
  // 2. CRAFT & PHOTO EDIT ACTIONS
  // ==========================================

  openEditCraftModal(craft: CraftItem, event: Event): void {
    event.stopPropagation();
    this.editingCraft = craft;
    this.craftForm = {
      title: craft.title,
      category: craft.category?.name || '',
      description: craft.description || '',
      basePrice: craft.basePrice,
      isLiveForAuction: craft.status === 'ACTIVE',
    };
    this.selectedCraftImageFile = null;
    this.craftImagePreviewUrl = craft.imageUrl ? resolveMediaUrl(craft.imageUrl) : null;
    this.craftEditError = '';
    this.isEditCraftModalOpen = true;
  }

  closeEditCraftModal(): void {
    this.isEditCraftModalOpen = false;
    this.editingCraft = null;
    this.selectedCraftImageFile = null;
    this.craftImagePreviewUrl = null;
    this.craftEditError = '';
  }

  onCraftImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      if (file.size > 10 * 1024 * 1024) {
        this.craftEditError = 'Craft photo must be less than 10 MB.';
        return;
      }
      this.selectedCraftImageFile = file;
      this.craftEditError = '';

      const reader = new FileReader();
      reader.onload = () => {
        this.craftImagePreviewUrl = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  saveCraftChanges(): void {
    if (!this.editingCraft) return;

    if (!this.craftForm.title?.trim()) {
      this.craftEditError = 'Craft title is required.';
      return;
    }
    if (!this.craftForm.basePrice || this.craftForm.basePrice <= 0) {
      this.craftEditError = 'Base price must be greater than zero.';
      return;
    }

    this.craftEditLoading = true;
    this.craftEditError = '';

    const formData = new FormData();
    formData.append('title', this.craftForm.title.trim());
    if (this.craftForm.category) {
      formData.append('category', this.craftForm.category.trim());
    }
    formData.append('description', this.craftForm.description || '');
    formData.append('basePrice', this.craftForm.basePrice.toString());
    formData.append('isLiveForAuction', this.craftForm.isLiveForAuction.toString());

    if (this.selectedCraftImageFile) {
      formData.append('image', this.selectedCraftImageFile);
    }

    this.craftService.updateCraftWithMedia(this.editingCraft.id, formData).subscribe({
      next: (updatedCraft) => {
        const index = this.crafts.findIndex((c) => c.id === updatedCraft.id);
        if (index !== -1) {
          this.crafts[index] = updatedCraft;
        }
        this.craftEditLoading = false;
        this.closeEditCraftModal();
        this.toastService.success(`🎨 Craft "${updatedCraft.title}" updated successfully!`);
      },
      error: (err) => {
        this.craftEditLoading = false;
        this.craftEditError = err.error?.message || 'Failed to update craft details.';
        this.toastService.error(this.craftEditError);
      },
    });
  }

  // ==========================================
  // 3. REEL EDIT & DELETE ACTIONS
  // ==========================================

  openEditReelModal(reel: CraftReelItem, event: Event): void {
    event.stopPropagation();
    this.editingReel = reel;
    this.reelForm = {
      title: reel.title,
      description: reel.description || '',
      videoUrl: reel.videoUrl,
      thumbnailUrl: reel.thumbnailUrl || '',
    };
    this.reelEditError = '';
    this.isEditReelModalOpen = true;
  }

  closeEditReelModal(): void {
    this.isEditReelModalOpen = false;
    this.editingReel = null;
    this.reelEditError = '';
  }

  saveReelChanges(): void {
    if (!this.editingReel) return;

    if (!this.reelForm.title?.trim()) {
      this.reelEditError = 'Reel title is required.';
      return;
    }

    this.reelEditLoading = true;
    this.reelEditError = '';

    this.craftReelService.updateReel(this.editingReel.id, this.reelForm).subscribe({
      next: (updatedReel) => {
        const index = this.reels.findIndex((r) => r.id === updatedReel.id);
        if (index !== -1) {
          this.reels[index] = updatedReel;
        }
        this.reelEditLoading = false;
        this.closeEditReelModal();
        this.toastService.success(`🎬 Reel "${updatedReel.title}" updated successfully!`);
      },
      error: (err) => {
        this.reelEditLoading = false;
        this.reelEditError = err.error?.message || 'Failed to update reel.';
        this.toastService.error(this.reelEditError);
      },
    });
  }

  deleteReel(id: number, event: Event): void {
    event.stopPropagation();
    if (!confirm('Are you sure you want to delete this craft reel?')) {
      return;
    }

    this.craftReelService.deleteReel(id).subscribe({
      next: () => {
        this.reels = this.reels.filter((r) => r.id !== id);
        this.totalReels = this.reels.length;
        this.totalViews = this.reels.reduce((sum, r) => sum + (r.views || 0), 0);
        this.totalLikes = this.reels.reduce((sum, r) => sum + (r.likes || 0), 0);
        this.toastService.success('Craft reel deleted from your studio.');
      },
      error: (err) => {
        console.error('Failed to delete reel:', err);
        this.toastService.error('Failed to delete craft reel.');
      },
    });
  }

  // ==========================================
  // 4. CRAFT STATUS & AUCTION ACTIONS
  // ==========================================

  updateOrderStatus(order: AuctionOrderItem, newStatus: string): void {
    const trackingNotes = prompt(`Enter courier/tracking details for order #CB-ORD-${order.id}:`, 'Shipped via Express Logistics');
    if (trackingNotes === null) return;

    this.auctionService.updateOrderStatus(order.id, newStatus, trackingNotes, 'Standard Express').subscribe({
      next: (updated) => {
        order.status = updated.status;
        this.toastService.success(`📦 Order #CB-ORD-${order.id} status updated to "${newStatus}"!`);
      },
      error: () => {
        this.toastService.error('Failed to update order status.');
      }
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
    if (!confirm('Are you sure you want to delete this craft? Associated auctions and craft reels will also be removed.')) {
      return;
    }

    this.craftService.deleteCraft(id).subscribe({
      next: () => {
        this.crafts = this.crafts.filter((c) => c.id !== id);
        this.totalCrafts = this.crafts.length;
        this.loadReelsData(); // Refresh reels list since associated reels are deleted
        this.toastService.success('Craft deleted from your studio.');
      },
      error: (err) => {
        console.error('Failed to delete craft:', err);
        const msg = err.error?.message || (typeof err.error === 'string' && err.error ? err.error : 'Failed to delete craft.');
        this.toastService.error(msg);
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

  onAvatarError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Crect width='100' height='100' rx='50' fill='%23ea580c'/%3E%3Ctext x='50' y='65' font-size='42' text-anchor='middle' fill='%23ffffff'%3E👨‍🎨%3C/text%3E%3C/svg%3E";
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
