import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuctionService, BidItem } from '../../services/auction.service';
import { AuthService } from '../../services/auth';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';

@Component({
  selector: 'app-my-bids',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './my-bids.html',
  styleUrl: './my-bids.css',
})
export class MyBids implements OnInit {
  bids: BidItem[] = [];
  loading = true;

  constructor(
    private auctionService: AuctionService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadMyBids();
  }

  loadMyBids(): void {
    this.loading = true;
    this.auctionService.getMyBids().subscribe({
      next: (data) => {
        this.bids = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load bids:', err);
        this.loading = false;
      },
    });
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString();
  }
}
