import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CategoryService, CategoryItem } from '../../../services/category.service';
import { CraftService, CraftItem } from '../../../services/craft.service';
import { CraftReelService, CraftReelItem } from '../../../services/craft-reel.service';
import { AuthService } from '../../../services/auth';
import { resolveMediaUrl } from '../../../services/api-config';

@Component({
  selector: 'app-home-content',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home-content.html',
  styleUrl: './home-content.css',
})
export class HomeContent implements OnInit {
  resolveMediaUrl = resolveMediaUrl;
  categories: CategoryItem[] = [];
  crafts: CraftItem[] = [];
  craftReels: CraftReelItem[] = [];
  isSeller = false;

  loading = true;

  categoryIcons: Record<string, string> = {
    'Pottery & Ceramics': '🏺',
    'Woodworking': '🪵',
    'Handmade Jewelry': '💍',
    'Paintings & Canvas': '🎨',
    'Textiles & Weaving': '🧵',
    'Leather Goods': '👜',
    'Glass Art': '✨',
    'Metalcraft & Sculptures': '🗿',
  };

  constructor(
    private categoryService: CategoryService,
    private craftService: CraftService,
    private craftReelService: CraftReelService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isSeller =
      this.authService.isLoggedIn() &&
      (this.authService.isSellerEnabled() || this.authService.isAdmin());
    this.loadHomeData();
  }

  loadHomeData(): void {
    this.loading = true;

    this.categoryService.getAllCategories().subscribe({
      next: (cats) => {
        this.categories = cats;
      },
      error: (err) => console.error('Failed to load categories:', err),
    });

    this.craftService.getAllCrafts().subscribe({
      next: (crafts) => {
        this.crafts = crafts;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load crafts:', err);
        this.loading = false;
      },
    });

    this.craftReelService.getHomeReels().subscribe({
      next: (reels) => {
        this.craftReels = reels;
      },
      error: (err) => console.error('Failed to load reels:', err),
    });
  }

  getCategoryIcon(name: string): string {
    return this.categoryIcons[name] || '🎁';
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  likeReel(reel: CraftReelItem, event: Event): void {
    event.stopPropagation();
    reel.likes = (reel.likes || 0) + 1;
    this.craftReelService.likeReel(reel.id).subscribe();
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400&q=80';
  }
}
