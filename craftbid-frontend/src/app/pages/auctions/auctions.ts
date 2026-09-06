import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuctionService, AuctionItem } from '../../services/auction.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';
import { resolveMediaUrl } from '../../services/api-config';

@Component({
  selector: 'app-auctions',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './auctions.html',
  styleUrl: './auctions.css',
})
export class Auctions implements OnInit, OnDestroy {
  resolveMediaUrl = resolveMediaUrl;
  auctions: AuctionItem[] = [];
  loading = true;
  timerInterval: any;

  constructor(
    private auctionService: AuctionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAuctions();
    this.timerInterval = setInterval(() => {
      // triggers change detection for countdowns
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  loadAuctions(): void {
    this.loading = true;
    this.auctionService.getActiveAuctions().subscribe({
      next: (data) => {
        this.auctions = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load active auctions:', err);
        this.loading = false;
      },
    });
  }

  getTimeRemaining(endTimeStr: string): string {
    const end = new Date(endTimeStr).getTime();
    const now = new Date().getTime();
    const diff = end - now;

    if (diff <= 0) {
      return 'Auction Ended';
    }

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

    return `${hours}h ${minutes}m ${seconds}s`;
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400&q=80';
  }
}
