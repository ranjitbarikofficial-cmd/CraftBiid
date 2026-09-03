import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SupportService, SupportTicketItem } from '../../services/support.service';
import { AuthService, UserAuth } from '../../services/auth';
import { ToastService } from '../../services/toast.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';

interface FAQ {
  q: string;
  a: string;
  category: string;
  open?: boolean;
}

@Component({
  selector: 'app-customer-service',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './customer-service.html',
  styleUrl: './customer-service.css',
})
export class CustomerServicePage implements OnInit {
  currentUser: UserAuth | null = null;
  searchQuery = '';

  ticketForm = {
    name: '',
    email: '',
    phone: '',
    category: 'AUCTION_BIDDING',
    subject: '',
    message: '',
  };
  submittingTicket = false;
  submittedTicket: SupportTicketItem | null = null;

  myTickets: SupportTicketItem[] = [];

  faqs: FAQ[] = [
    {
      q: 'How does the Pay-to-Bid deposit system work?',
      a: 'To maintain serious bidding and protect artisans from fake bids, collectors pay the item base price (e.g. ₹1,500) to join the auction room. Maximum 10 participants are allowed per room.',
      category: 'auctions',
      open: true,
    },
    {
      q: 'How does differential bidding work?',
      a: 'You only pay the difference when raising a bid! For instance, if you already paid ₹1,500 and raise your bid to ₹1,800, you only pay the incremental differential of ₹300.',
      category: 'auctions',
      open: false,
    },
    {
      q: 'When and how are refunds processed for non-winning participants?',
      a: '100% of your deposits and differential bids are automatically refunded via instant UPI/bank reversal the moment the auction finishes if you are not the highest bidder. Zero deduction.',
      category: 'payments',
      open: false,
    },
    {
      q: 'What is the 1-minute live turn timer?',
      a: 'Whenever any participant places a higher bid, a 60-second turn countdown begins. If no other collector places a higher bid before the countdown expires, the highest bidder wins.',
      category: 'auctions',
      open: false,
    },
    {
      q: 'How do artisans receive their payouts?',
      a: 'Artisans receive 90% net payout directly into their bank account/UPI upon winner delivery address confirmation. CraftBid retains a transparent 10% platform facilitation fee.',
      category: 'artisan',
      open: false,
    },
    {
      q: 'How do I track my order delivery?',
      a: 'Once you win an auction and submit your delivery address, the artisan prepares dispatch within 2-3 business days. You can monitor status under your Profile and Orders.',
      category: 'shipping',
      open: false,
    },
  ];

  constructor(
    private supportService: SupportService,
    private authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
      if (user) {
        this.ticketForm.name = user.name || '';
        this.ticketForm.email = user.email || '';
        this.loadMyTickets();
      }
    });
  }

  loadMyTickets(): void {
    this.supportService.getMyTickets().subscribe({
      next: (tickets) => {
        this.myTickets = tickets;
      },
      error: (err) => console.error('Failed to load my tickets:', err),
    });
  }

  toggleFaq(faq: FAQ): void {
    faq.open = !faq.open;
  }

  get filteredFaqs(): FAQ[] {
    if (!this.searchQuery.trim()) return this.faqs;
    const q = this.searchQuery.toLowerCase();
    return this.faqs.filter(
      (f) => f.q.toLowerCase().includes(q) || f.a.toLowerCase().includes(q)
    );
  }

  submitTicket(): void {
    if (!this.ticketForm.subject.trim() || !this.ticketForm.message.trim()) {
      this.toastService.error('Please enter a subject and message.');
      return;
    }

    this.submittingTicket = true;
    this.supportService.createTicket(this.ticketForm).subscribe({
      next: (ticket) => {
        this.submittingTicket = false;
        this.submittedTicket = ticket;
        this.toastService.success(`🎉 Ticket created! Ref: ${ticket.ticketRef}`);
        this.loadMyTickets();
      },
      error: (err) => {
        this.submittingTicket = false;
        console.error('Failed to create ticket:', err);
        this.toastService.error('Failed to create ticket.');
      },
    });
  }

  resetForm(): void {
    this.submittedTicket = null;
    this.ticketForm.subject = '';
    this.ticketForm.message = '';
  }
}
