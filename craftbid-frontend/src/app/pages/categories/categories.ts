import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CategoryService, CategoryItem } from '../../services/category.service';
import { CraftService, CraftItem } from '../../services/craft.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './categories.html',
  styleUrl: './categories.css',
})
export class Categories implements OnInit {
  categories: CategoryItem[] = [];
  selectedCategory: CategoryItem | null = null;
  crafts: CraftItem[] = [];
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
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (cats) => {
        this.categories = cats;
        this.route.queryParams.subscribe((params) => {
          const catId = params['id'];
          if (catId) {
            const found = this.categories.find((c) => c.id === Number(catId));
            if (found) {
              this.selectCategory(found);
              return;
            }
          }
          if (this.categories.length > 0) {
            this.selectCategory(this.categories[0]);
          } else {
            this.loading = false;
          }
        });
      },
      error: (err) => {
        console.error('Failed to load categories:', err);
        this.loading = false;
      },
    });
  }

  selectCategory(category: CategoryItem): void {
    this.selectedCategory = category;
    this.loading = true;
    this.craftService.getCraftsByCategory(category.id).subscribe({
      next: (data) => {
        this.crafts = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load category crafts:', err);
        this.loading = false;
      },
    });
  }

  getCategoryIcon(name: string): string {
    return this.categoryIcons[name] || '🎁';
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400&q=80';
  }
}
