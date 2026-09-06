import { Component, OnInit, ElementRef, ViewChildren, QueryList, AfterViewInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CraftReelService, CraftReelItem } from '../../services/craft-reel.service';
import { FollowService } from '../../services/follow.service';
import { AuctionService } from '../../services/auction.service';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast.service';
import { resolveMediaUrl } from '../../services/api-config';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';

@Component({
  selector: 'app-reels',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './reels.html',
  styleUrl: './reels.css',
})
export class Reels implements OnInit, AfterViewInit {
  resolveMediaUrl = resolveMediaUrl;
  @ViewChildren('videoElement') videoElements!: QueryList<ElementRef<HTMLVideoElement>>;
  @ViewChildren('reelCard') reelCards!: QueryList<ElementRef<HTMLDivElement>>;

  // Feed selection: 'for-you' vs 'following'
  feedType: 'for-you' | 'following' = 'for-you';

  reels: CraftReelItem[] = [];
  loading = true;
  activeIndex = 0;
  isMuted = false;

  // Social states
  likedReels: Set<number> = new Set();
  interestedReels: Set<number> = new Set();
  followingArtisans: Set<number> = new Set();
  artisanFollowerCounts: Map<number, number> = new Map();

  // Active auctions mapping (craftId -> auctionId)
  activeAuctionsMap: Map<number, number> = new Map();

  constructor(
    private craftReelService: CraftReelService,
    private followService: FollowService,
    private auctionService: AuctionService,
    public authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadReels();
    this.loadActiveAuctions();
  }

  ngAfterViewInit(): void {
    // Video viewport observer handled reactively
  }

  switchFeed(feed: 'for-you' | 'following'): void {
    if (this.feedType === feed) return;

    if (feed === 'following' && !this.authService.isLoggedIn()) {
      this.toastService.info('Please log in to see reels from artisans you follow!');
      this.router.navigate(['/login']);
      return;
    }

    this.feedType = feed;
    this.activeIndex = 0;
    this.loadReels();
  }

  loadReels(): void {
    this.loading = true;
    if (this.feedType === 'following') {
      this.followService.getFollowingReels().subscribe({
        next: (data) => {
          this.reels = data;
          this.loading = false;
          this.checkFollowStatuses();
          setTimeout(() => this.playVideoAtIndex(0), 300);
        },
        error: (err) => {
          console.error('Failed to load following reels:', err);
          this.loading = false;
        },
      });
    } else {
      this.craftReelService.getHomeReels().subscribe({
        next: (data) => {
          this.reels = data;
          this.loading = false;
          this.checkFollowStatuses();
          setTimeout(() => this.playVideoAtIndex(0), 300);
        },
        error: (err) => {
          console.error('Failed to load reels:', err);
          this.loading = false;
        },
      });
    }
  }

  loadActiveAuctions(): void {
    this.auctionService.getActiveAuctions().subscribe({
      next: (auctions) => {
        this.activeAuctionsMap.clear();
        auctions.forEach((a) => {
          if (a.craft && a.craft.id && a.status === 'ACTIVE') {
            this.activeAuctionsMap.set(a.craft.id, a.id);
          }
        });
      },
      error: (err) => console.log('Could not load live auctions mapping:', err),
    });
  }

  getAuctionIdForCraft(craftId?: number): number | null {
    if (!craftId) return null;
    return this.activeAuctionsMap.get(craftId) || null;
  }

  checkFollowStatuses(): void {
    if (!this.authService.isLoggedIn()) return;

    this.reels.forEach((reel) => {
      const artisanUserId = reel.artisan?.user?.id;
      if (artisanUserId) {
        this.followService.getFollowStatus(artisanUserId).subscribe({
          next: (res) => {
            if (res.following) {
              this.followingArtisans.add(artisanUserId);
            } else {
              this.followingArtisans.delete(artisanUserId);
            }
            this.artisanFollowerCounts.set(artisanUserId, res.followerCount || 0);
          },
        });
      }
    });
  }

  // Follow / Unfollow toggle
  toggleFollow(artisanUserId?: number, event?: Event): void {
    if (event) event.stopPropagation();

    if (!this.authService.isLoggedIn()) {
      this.toastService.info('Please log in to follow this artisan!');
      this.router.navigate(['/login']);
      return;
    }

    if (!artisanUserId) return;

    this.followService.toggleFollow(artisanUserId).subscribe({
      next: (res) => {
        if (res.following) {
          this.followingArtisans.add(artisanUserId);
          this.toastService.success(res.message || 'Artisan followed! You will see their new crafts.');
        } else {
          this.followingArtisans.delete(artisanUserId);
          this.toastService.info(res.message || 'Unfollowed artisan.');
        }
        this.artisanFollowerCounts.set(artisanUserId, res.followerCount || 0);
      },
      error: (err) => {
        console.error('Follow toggle error:', err);
        this.toastService.error(err.error?.message || 'Could not update follow status.');
      },
    });
  }

  isFollowingArtisan(artisanUserId?: number): boolean {
    if (!artisanUserId) return false;
    return this.followingArtisans.has(artisanUserId);
  }

  getFollowerCount(artisanUserId?: number): number {
    if (!artisanUserId) return 0;
    return this.artisanFollowerCounts.get(artisanUserId) || 0;
  }

  // Like Toggle
  likeReel(reel: CraftReelItem, event: Event): void {
    event.stopPropagation();
    if (this.likedReels.has(reel.id)) {
      this.likedReels.delete(reel.id);
      reel.likes = Math.max(0, (reel.likes || 1) - 1);
    } else {
      this.likedReels.add(reel.id);
      reel.likes = (reel.likes || 0) + 1;
      this.toastService.info('❤️ Liked craft process reel!');
      this.craftReelService.likeReel(reel.id).subscribe();
    }
  }

  isLiked(reelId: number): boolean {
    return this.likedReels.has(reelId);
  }

  // Interested Toggle
  toggleInterested(reel: CraftReelItem, event: Event): void {
    event.stopPropagation();
    if (!this.authService.isLoggedIn()) {
      this.toastService.info('Please sign in to save your interested crafts!');
      this.router.navigate(['/login']);
      return;
    }

    if (this.interestedReels.has(reel.id)) {
      this.interestedReels.delete(reel.id);
      this.toastService.info('Removed from your interested list.');
    } else {
      this.interestedReels.add(reel.id);
      this.toastService.success('😍 Marked as Interested! Saved to your wishlist.');
    }
  }

  isInterested(reelId: number): boolean {
    return this.interestedReels.has(reelId);
  }

  // Universal Creator Action: Jump straight to Upload Craft / Reel
  goToUpload(): void {
    if (!this.authService.isLoggedIn()) {
      this.toastService.info('Please log in to upload your crafts & reels!');
      this.router.navigate(['/login']);
      return;
    }

    if (!this.authService.isSellerEnabled()) {
      // Auto-enable artisan with user's name
      const user = this.authService.getCurrentUser();
      this.authService
        .enableSeller({
          shopName: `${user?.name || 'Creator'} Studio`,
          craftType: 'Handmade Crafts',
          city: 'India',
        })
        .subscribe({
          next: () => {
            this.toastService.success('🎨 Welcome! Your Artisan Studio is now active.');
            this.router.navigate(['/artisan/upload-craft']);
          },
          error: () => {
            this.router.navigate(['/artisan-dashboard']);
          },
        });
    } else {
      this.router.navigate(['/artisan/upload-craft']);
    }
  }

  // Keyboard navigation (Arrow keys)
  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'ArrowDown' || event.key === 'PageDown') {
      event.preventDefault();
      this.goToNextReel();
    } else if (event.key === 'ArrowUp' || event.key === 'PageUp') {
      event.preventDefault();
      this.goToPrevReel();
    } else if (event.key === 'm' || event.key === 'M') {
      this.toggleMute();
    }
  }

  // Auto-advance / Auto-scroll when video completes playback
  onVideoEnded(index: number): void {
    this.toastService.info('🎬 Next craft reel starting automatically...');
    if (index < this.reels.length - 1) {
      this.scrollToIndex(index + 1);
    } else {
      this.scrollToIndex(0);
    }
  }

  goToNextReel(): void {
    if (this.activeIndex < this.reels.length - 1) {
      this.scrollToIndex(this.activeIndex + 1);
    }
  }

  goToPrevReel(): void {
    if (this.activeIndex > 0) {
      this.scrollToIndex(this.activeIndex - 1);
    }
  }

  scrollToIndex(index: number): void {
    if (index < 0 || index >= this.reels.length) return;
    this.activeIndex = index;

    const cards = this.reelCards?.toArray();
    if (cards && cards[index]) {
      cards[index].nativeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }

    this.playVideoAtIndex(index);
  }

  playVideoAtIndex(index: number): void {
    const videos = this.videoElements?.toArray();
    if (!videos) return;

    videos.forEach((v, i) => {
      const vid = v.nativeElement;
      if (i === index) {
        vid.currentTime = 0;
        vid.muted = this.isMuted;
        vid.play().catch((err) => console.log('Autoplay handled:', err));
        this.trackView(this.reels[index]);
      } else {
        vid.pause();
      }
    });
  }

  togglePlayPause(video: HTMLVideoElement): void {
    if (video.paused) {
      video.play();
    } else {
      video.pause();
    }
  }

  toggleMute(): void {
    this.isMuted = !this.isMuted;
    const videos = this.videoElements?.toArray();
    if (videos) {
      videos.forEach((v) => (v.nativeElement.muted = this.isMuted));
    }
  }

  trackView(reel: CraftReelItem): void {
    if (!reel) return;
    this.craftReelService.incrementViews(reel.id).subscribe({
      next: (updated) => {
        reel.views = updated.views;
      },
    });
  }

  formatPrice(price?: number): string {
    if (!price) return '';
    return '₹' + price.toLocaleString('en-IN');
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=100&q=80';
  }
}
