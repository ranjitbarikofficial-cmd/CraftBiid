import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CraftService, CraftItem } from '../../services/craft.service';
import { CraftReelService, CraftReelItem } from '../../services/craft-reel.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';

@Component({
  selector: 'app-craft-details',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './craft-details.html',
  styleUrl: './craft-details.css',
})
export class CraftDetails implements OnInit {
  craft: CraftItem | null = null;
  reels: CraftReelItem[] = [];
  loading = true;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private craftService: CraftService,
    private craftReelService: CraftReelService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      const id = Number(params['id']);
      if (id) {
        this.loadCraft(id);
      }
    });
  }

  loadCraft(id: number): void {
    this.loading = true;
    this.craftService.getCraftById(id).subscribe({
      next: (craft) => {
        this.craft = craft;
        this.loading = false;
        this.loadReels(id);
      },
      error: (err) => {
        console.error('Failed to load craft:', err);
        this.loading = false;
        this.errorMessage = 'Craft not found or removed.';
      },
    });
  }

  loadReels(craftId: number): void {
    this.craftReelService.getReelsByCraftId(craftId).subscribe({
      next: (reels) => {
        this.reels = reels;
      },
      error: (err) => {
        console.error('Failed to load craft reels:', err);
      },
    });
  }

  formatPrice(price: number): string {
    return '₹' + price?.toLocaleString('en-IN');
  }

  likeReel(reel: CraftReelItem): void {
    reel.likes = (reel.likes || 0) + 1;
    this.craftReelService.likeReel(reel.id).subscribe();
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
