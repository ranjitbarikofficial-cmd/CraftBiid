import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
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
  errorMessage: string | null = null;
  timerInterval: any = null;
  private isFetching = false;

  constructor(
    private auctionService: AuctionService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAuctions();
    this.timerInterval = setInterval(() => {
      this.cdr.markForCheck();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  loadAuctions(force = false): void {
    if (this.isFetching && !force) {
      return;
    }

    this.isFetching = true;
    this.loading = true;
    this.errorMessage = null;
    this.cdr.markForCheck();

    this.auctionService
      .getActiveAuctions()
      .pipe(
        finalize(() => {
          this.loading = false;
          this.isFetching = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (response: any) => {
          this.auctions = this.extractAuctionArray(response);
          this.errorMessage = null;
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          console.error('Failed to load active auctions:', err);
          this.errorMessage = 'Unable to load live auctions right now. Please try again.';
          this.cdr.markForCheck();
        },
      });
  }

  private extractAuctionArray(response: any): AuctionItem[] {
    if (!response) return [];
    if (Array.isArray(response)) return response;
    if (Array.isArray(response.content)) return response.content;
    if (Array.isArray(response.data)) return response.data;
    if (Array.isArray(response.auctions)) return response.auctions;
    if (Array.isArray(response.items)) return response.items;
    if (typeof response === 'object') {
      for (const key of Object.keys(response)) {
        if (Array.isArray(response[key])) {
          return response[key];
        }
      }
    }
    return [];
  }

  isAuctionLive(auction: AuctionItem): boolean {
    return !!auction.liveTurnActive;
  }

  isParticipationOpen(auction: AuctionItem): boolean {
    return !auction.liveTurnActive && !!auction.participationDeadline && (auction.currentParticipantsCount || 0) > 0;
  }

  isWaitingForFirstParticipant(auction: AuctionItem): boolean {
    return !auction.liveTurnActive && (!auction.participationDeadline || (auction.currentParticipantsCount || 0) === 0);
  }

  getTimeRemaining(endTimeStr?: string | null): string {
    if (!endTimeStr) return 'Live Now';
    const end = new Date(endTimeStr).getTime();
    if (isNaN(end)) return 'Live Now';

    const now = Date.now();
    const diff = end - now;

    if (diff <= 0) {
      return 'Window Closed';
    }

    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

    if (hours > 24) {
      const days = Math.floor(hours / 24);
      const remHours = hours % 24;
      return `${days}d ${remHours}h left`;
    }

    return `${hours}h ${minutes}m ${seconds}s`;
  }

  formatPrice(price?: number | null): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400&q=80';
  }
}

