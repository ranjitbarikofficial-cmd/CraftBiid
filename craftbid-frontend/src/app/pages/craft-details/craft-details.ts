import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import { CraftService, CraftItem } from '../../services/craft.service';
import { CraftReelService, CraftReelItem } from '../../services/craft-reel.service';
import { AuctionService } from '../../services/auction.service';
import { AuthService } from '../../services/auth';
import { FollowService } from '../../services/follow.service';
import { ToastService } from '../../services/toast.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';
import { resolveMediaUrl } from '../../services/api-config';

@Component({
  selector: 'app-craft-details',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './craft-details.html',
  styleUrl: './craft-details.css',
})
export class CraftDetails implements OnInit, OnDestroy {
  resolveMediaUrl = resolveMediaUrl;

  craftId: number | null = null;
  craft: CraftItem | null = null;
  reels: CraftReelItem[] = [];
  activeAuction: any | null = null;

  loading = true;
  errorMessage = '';
  errorType: 'NOT_FOUND' | 'NETWORK' | 'SERVER' | 'GENERIC' = 'GENERIC';
  isWishlisted = false;
  isFollowing = false;
  followerCount = 0;

  private routeSub: Subscription | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private craftService: CraftService,
    private craftReelService: CraftReelService,
    private auctionService: AuctionService,
    public authService: AuthService,
    private followService: FollowService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe((paramMap) => {
      const idStr = paramMap.get('id');
      if (idStr) {
        const id = Number(idStr);
        if (!isNaN(id) && id > 0) {
          if (this.craftId !== id || !this.craft) {
            this.craftId = id;
            this.loadCraft(id);
          }
        } else {
          this.loading = false;
          this.errorMessage = 'Invalid craft identifier specified.';
          this.errorType = 'NOT_FOUND';
        }
      } else {
        this.loading = false;
        this.errorMessage = 'No craft ID provided in URL.';
        this.errorType = 'NOT_FOUND';
      }
    });
  }

  ngOnDestroy(): void {
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  loadCraft(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.craft = null;

    this.craftService
      .getCraftById(id)
      .pipe(
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: (craft) => {
          if (!craft || !craft.id) {
            this.errorMessage = 'Craft item not found or has been removed.';
            this.errorType = 'NOT_FOUND';
            this.craft = null;
            return;
          }

          this.craft = craft;

          // Secondary non-blocking enrichments
          this.loadReels(id);
          this.loadActiveAuction(id);
          this.checkFollowStatus();
        },
        error: (error) => {
          console.error('Craft loading failed', {
            status: error?.status,
            message: error?.message,
          });

          this.craft = null;

          if (error?.status === 404) {
            this.errorMessage = 'Craft not found. This craft creation may have been deleted by the artisan.';
            this.errorType = 'NOT_FOUND';
          } else if (error?.status === 0 || error?.status === 504 || error?.status === 502) {
            this.errorMessage = 'Unable to connect to the server. Please check your internet connection.';
            this.errorType = 'NETWORK';
          } else {
            this.errorMessage =
              error?.error?.message || 'Unable to load craft details at this time. Please try again.';
            this.errorType = 'SERVER';
          }
        },
      });
  }

  retry(): void {
    if (this.craftId) {
      this.loadCraft(this.craftId);
    }
  }

  loadReels(craftId: number): void {
    this.craftReelService.getReelsByCraftId(craftId).subscribe({
      next: (reels) => {
        this.reels = (reels || []).filter((r) => r.videoUrl && r.videoUrl.trim().length > 0);
      },
      error: (err) => {
        console.debug('Could not load craft reels:', err);
      },
    });
  }

  loadActiveAuction(craftId: number): void {
    this.auctionService.getActiveAuctions().subscribe({
      next: (auctions) => {
        const found = (auctions || []).find(
          (a) => a.craft && a.craft.id === craftId && a.status !== 'ENDED' && a.status !== 'CANCELLED'
        );
        this.activeAuction = found || null;
      },
      error: (err) => {
        console.debug('Could not query active auction status:', err);
      },
    });
  }

  checkFollowStatus(): void {
    const sellerUserId = this.craft?.seller?.id;
    if (!sellerUserId || !this.authService.isLoggedIn()) return;

    this.followService.getFollowStatus(sellerUserId).subscribe({
      next: (res) => {
        this.isFollowing = !!res.following;
        this.followerCount = res.followerCount || 0;
      },
      error: () => {},
    });
  }

  toggleFollow(): void {
    const sellerUserId = this.craft?.seller?.id;
    if (!sellerUserId) return;

    if (!this.authService.isLoggedIn()) {
      this.toastService.info('Please log in to follow this artisan!');
      this.router.navigate(['/login']);
      return;
    }

    this.followService.toggleFollow(sellerUserId).subscribe({
      next: (res) => {
        this.isFollowing = !!res.following;
        this.followerCount = res.followerCount || 0;
        if (this.isFollowing) {
          this.toastService.success(res.message || 'Artisan followed!');
        } else {
          this.toastService.info(res.message || 'Unfollowed artisan.');
        }
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Could not update follow status.');
      },
    });
  }

  toggleWishlist(): void {
    this.isWishlisted = !this.isWishlisted;
    if (this.isWishlisted) {
      this.toastService.success('❤️ Added to your saved wishlist!');
    } else {
      this.toastService.info('Removed from your saved wishlist.');
    }
  }

  formatPrice(price?: number): string {
    if (!price && price !== 0) return '₹0';
    return '₹' + price.toLocaleString('en-IN');
  }

  likeReel(reel: CraftReelItem): void {
    reel.likes = (reel.likes || 0) + 1;
    this.craftReelService.likeReel(reel.id).subscribe();
    this.toastService.info('❤️ Liked craft process reel!');
  }

  onVideoPlay(reel: CraftReelItem): void {
    this.craftReelService.incrementViews(reel.id).subscribe({
      next: (updated) => {
        reel.views = updated.views;
      },
    });
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600&q=80';
  }
}
