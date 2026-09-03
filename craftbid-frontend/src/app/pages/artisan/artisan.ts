import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';

@Component({
  selector: 'app-artisan',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './artisan.html',
  styleUrl: './artisan.css',
})
export class Artisan implements OnInit {
  shopName = '';
  craftType = 'Pottery';
  city = '';

  loading = false;
  errorMessage = '';

  popularCrafts = [
    { name: 'Pottery', icon: '🏺' },
    { name: 'Painting', icon: '🎨' },
    { name: 'Woodwork', icon: '🪵' },
    { name: 'Jewellery', icon: '💍' },
    { name: 'Handloom', icon: '🧵' },
    { name: 'Textile', icon: '🧶' },
    { name: 'Sculpture', icon: '🗿' },
    { name: 'Home Decor', icon: '🏠' },
  ];

  constructor(
    public authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (
      this.authService.isLoggedIn() &&
      (this.authService.isSellerEnabled() || this.authService.isAdmin())
    ) {
      this.router.navigate(['/artisan-dashboard'], { replaceUrl: true });
    }

    const user = this.authService.getCurrentUser();
    if (user && !this.shopName) {
      this.shopName = `${user.name}'s Studio`;
    }
  }

  selectCraftType(type: string): void {
    this.craftType = type;
  }

  enableSellerAccount(): void {
    this.errorMessage = '';

    if (!this.shopName.trim()) {
      this.errorMessage = 'Please enter your workshop or studio name.';
      return;
    }

    if (!this.craftType) {
      this.errorMessage = 'Please select your primary craft specialty.';
      return;
    }

    if (!this.city.trim()) {
      this.errorMessage = 'Please enter your workshop city or state.';
      return;
    }

    if (!this.authService.isLoggedIn()) {
      this.errorMessage = 'Please log in before activating your Artisan Studio.';
      this.router.navigate(['/login']);
      return;
    }

    this.loading = true;

    this.authService
      .enableSeller({
        shopName: this.shopName.trim(),
        craftType: this.craftType.trim(),
        city: this.city.trim(),
      })
      .subscribe({
        next: () => {
          this.loading = false;
          this.toastService.success('🎉 Welcome to CraftBid Artisan Studio!');
          this.router.navigate(['/artisan-dashboard']);
        },
        error: (error) => {
          this.loading = false;
          console.error('Artisan activation error:', error);

          if (error.status === 401) {
            this.errorMessage = 'Your session has expired. Please login again.';
          } else if (error.status === 403) {
            this.errorMessage = 'You are not authorized to become an Artisan.';
          } else {
            this.errorMessage =
              error.error?.message ||
              error.error ||
              (typeof error.error === 'string'
                ? error.error
                : 'Unable to activate Artisan account. Please try again.');
          }
        },
      });
  }
}
