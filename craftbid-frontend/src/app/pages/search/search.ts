import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CraftService, CraftItem, AutocompleteItem } from '../../services/craft.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';
import { resolveMediaUrl } from '../../services/api-config';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './search.html',
  styleUrl: './search.css',
})
export class Search implements OnInit {
  resolveMediaUrl = resolveMediaUrl;
  keyword = '';
  crafts: CraftItem[] = [];
  suggestions: AutocompleteItem[] = [];
  loading = false;
  minPrice: number | null = null;
  maxPrice: number | null = null;

  constructor(
    private craftService: CraftService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.keyword = params['q'] || '';
      this.performSearch();
    });
  }

  onKeywordInput(): void {
    if (!this.keyword || this.keyword.trim().length < 1) {
      this.suggestions = [];
      return;
    }

    this.craftService.getAutocompleteSuggestions(this.keyword, 6).subscribe({
      next: (items) => {
        this.suggestions = items;
      },
      error: () => {
        this.suggestions = [];
      },
    });
  }

  selectSuggestion(item: AutocompleteItem): void {
    this.keyword = item.suggestion;
    this.suggestions = [];
    this.performSearch();
  }

  performSearch(): void {
    this.suggestions = [];
    this.loading = true;
    this.craftService
      .searchCrafts({
        keyword: this.keyword,
        minPrice: this.minPrice || undefined,
        maxPrice: this.maxPrice || undefined,
      })
      .subscribe({
        next: (data) => {
          this.crafts = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Search error:', err);
          this.loading = false;
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
}

