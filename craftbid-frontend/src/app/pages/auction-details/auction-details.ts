import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  AuctionService,
  AuctionItem,
  BidItem,
  AuctionParticipantItem,
  AuctionOrderItem,
  SubmitAddressPayload
} from '../../services/auction.service';
import { AuthService, UserAuth } from '../../services/auth';
import { ToastService } from '../../services/toast.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';
import { resolveMediaUrl } from '../../services/api-config';

@Component({
  selector: 'app-auction-details',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './auction-details.html',
  styleUrl: './auction-details.css',
})
export class AuctionDetails implements OnInit, OnDestroy {
  resolveMediaUrl = resolveMediaUrl;
  auction: AuctionItem | null = null;
  bids: BidItem[] = [];
  participants: AuctionParticipantItem[] = [];
  currentUser: UserAuth | null = null;
  currentParticipant: AuctionParticipantItem | null = null;
  auctionOrder: AuctionOrderItem | null = null;

  bidAmount: number | null = null;
  minNextBid: number = 0;
  differentialToPay: number = 0;

  // Modals & States
  isJoinModalOpen = false;
  selectedPaymentMethod = 'UPI';
  isAddressModalOpen = false;
  addressForm: SubmitAddressPayload = {
    fullName: '',
    streetAddress: '',
    city: '',
    state: '',
    pincode: '',
    phone: '',
  };

  loading = true;
  joining = false;
  bidding = false;
  submittingAddress = false;
  errorMessage = '';
  successMessage = '';

  // 1-minute turn timer state
  secondsRemainingInTurn = 60;
  timerInterval: any;
  pollingInterval: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auctionService: AuctionService,
    private authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.route.params.subscribe((params) => {
      const id = Number(params['id']);
      if (id) {
        this.loadAuction(id);
      }
    });

    this.timerInterval = setInterval(() => {
      this.updateTurnTimer();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
    if (this.pollingInterval) clearInterval(this.pollingInterval);
  }

  loadAuction(id: number): void {
    this.loading = true;
    this.auctionService.getAuctionById(id).subscribe({
      next: (data) => {
        this.auction = data;
        this.calculateMinNextBid();
        this.loading = false;
        this.loadBids(id);
        this.loadParticipants(id);
        this.loadOrder(id);

        if (!this.pollingInterval) {
          this.pollingInterval = setInterval(() => {
            this.refreshData(id);
          }, 3000);
        }
      },
      error: (err) => {
        console.error('Failed to load auction:', err);
        this.loading = false;
        this.errorMessage = 'Auction not found.';
      },
    });
  }

  loadBids(auctionId: number): void {
    this.auctionService.getAuctionBids(auctionId).subscribe({
      next: (data) => (this.bids = data),
      error: (err) => console.error('Failed to load bids:', err),
    });
  }

  loadParticipants(auctionId: number): void {
    this.auctionService.getParticipants(auctionId).subscribe({
      next: (data) => {
        this.participants = data;
        if (this.currentUser) {
          this.currentParticipant =
            this.participants.find((p) => p.user.id === this.currentUser!.userId) || null;
        }
        this.updateDifferentialToPay();
      },
      error: (err) => console.error('Failed to load participants:', err),
    });
  }

  loadOrder(auctionId: number): void {
    this.auctionService.getAuctionOrder(auctionId).subscribe({
      next: (order) => (this.auctionOrder = order),
      error: () => (this.auctionOrder = null),
    });
  }

  refreshData(auctionId: number): void {
    this.auctionService.getAuctionById(auctionId).subscribe({
      next: (updatedAuction) => {
        this.auction = updatedAuction;
        this.calculateMinNextBid();
      },
    });

    this.auctionService.getAuctionBids(auctionId).subscribe({
      next: (bids) => (this.bids = bids),
    });

    this.auctionService.getParticipants(auctionId).subscribe({
      next: (participants) => {
        this.participants = participants;
        if (this.currentUser) {
          this.currentParticipant =
            this.participants.find((p) => p.user.id === this.currentUser!.userId) || null;
        }
        this.updateDifferentialToPay();
      },
    });
  }

  updateTurnTimer(): void {
    if (!this.auction || this.auction.status !== 'ACTIVE' || !this.auction.turnDeadline) {
      this.secondsRemainingInTurn = 60;
      return;
    }

    const deadline = new Date(this.auction.turnDeadline).getTime();
    const now = new Date().getTime();
    const diffSeconds = Math.max(0, Math.floor((deadline - now) / 1000));
    this.secondsRemainingInTurn = diffSeconds;

    if (diffSeconds === 0 && this.auction.status === 'ACTIVE') {
      this.refreshData(this.auction.id);
    }
  }

  calculateMinNextBid(): void {
    if (!this.auction) return;
    if (this.auction.totalBids === 0) {
      this.minNextBid = this.auction.startingPrice;
    } else {
      this.minNextBid = this.auction.currentHighestBid + (this.auction.minBidIncrement || 50);
    }
    if (!this.bidAmount || this.bidAmount < this.minNextBid) {
      this.bidAmount = this.minNextBid;
    }
    this.updateDifferentialToPay();
  }

  onBidAmountChange(): void {
    this.updateDifferentialToPay();
  }

  updateDifferentialToPay(): void {
    if (!this.bidAmount) {
      this.differentialToPay = 0;
      return;
    }
    const alreadyPaid = this.currentParticipant ? this.currentParticipant.totalAmountPaid : 0;
    this.differentialToPay = Math.max(0, this.bidAmount - alreadyPaid);
  }

  // ==========================================
  // JOIN AUCTION (PAY BASE DEPOSIT)
  // ==========================================

  openJoinModal(): void {
    if (!this.authService.isLoggedIn()) {
      this.toastService.warning('Please login before joining the auction');
      this.router.navigate(['/login']);
      return;
    }
    this.isJoinModalOpen = true;
  }

  closeJoinModal(): void {
    this.isJoinModalOpen = false;
  }

  confirmJoinAuction(): void {
    if (!this.auction) return;
    this.joining = true;

    this.auctionService.joinAuctionWithDeposit(this.auction.id, this.selectedPaymentMethod).subscribe({
      next: (participant) => {
        this.joining = false;
        this.currentParticipant = participant;
        this.closeJoinModal();
        this.toastService.success(`🎉 Base deposit of ₹${participant.basePricePaid} paid! You joined the auction room.`);
        this.refreshData(this.auction!.id);
      },
      error: (err) => {
        this.joining = false;
        const msg = err.error?.message || err.error || 'Failed to join auction.';
        this.toastService.error(msg);
      },
    });
  }

  // ==========================================
  // PLACE DIFFERENTIAL BID
  // ==========================================

  placeBid(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.authService.isLoggedIn()) {
      this.toastService.warning('Please login before placing a bid');
      this.router.navigate(['/login']);
      return;
    }

    if (!this.currentParticipant) {
      this.openJoinModal();
      return;
    }

    if (!this.bidAmount || this.bidAmount < this.minNextBid) {
      this.toastService.error(`Bid must be at least ₹${this.minNextBid}`);
      return;
    }

    if (!this.auction) return;

    this.bidding = true;
    this.auctionService.placeDifferentialBid(this.auction.id, this.bidAmount).subscribe({
      next: () => {
        this.bidding = false;
        this.toastService.success(`🎉 Differential ₹${this.differentialToPay} paid! Highest Bid set to ₹${this.bidAmount}`);
        this.refreshData(this.auction!.id);
      },
      error: (err) => {
        this.bidding = false;
        const msg = err.error?.message || err.error || 'Failed to place bid.';
        this.toastService.error(msg);
      },
    });
  }

  // ==========================================
  // SUBMIT DELIVERY ADDRESS (WINNER)
  // ==========================================

  openAddressModal(): void {
    this.isAddressModalOpen = true;
  }

  closeAddressModal(): void {
    this.isAddressModalOpen = false;
  }

  submitAddress(): void {
    if (!this.auction) return;
    if (!this.addressForm.fullName || !this.addressForm.streetAddress || !this.addressForm.city || !this.addressForm.pincode || !this.addressForm.phone) {
      this.toastService.error('Please fill in all delivery address fields');
      return;
    }

    this.submittingAddress = true;
    this.auctionService.submitDeliveryAddress(this.auction.id, this.addressForm).subscribe({
      next: (order) => {
        this.submittingAddress = false;
        this.auctionOrder = order;
        this.closeAddressModal();
        this.toastService.success('📦 Delivery address confirmed! Artisan notified for shipping.');
      },
      error: (err) => {
        this.submittingAddress = false;
        const msg = err.error?.message || err.error || 'Failed to submit address.';
        this.toastService.error(msg);
      },
    });
  }

  isCurrentUserWinner(): boolean {
    return (
      !!this.auction &&
      this.auction.status === 'ENDED' &&
      !!this.currentUser &&
      this.auction.winningBidder?.id === this.currentUser.userId
    );
  }

  isCurrentUserRefunded(): boolean {
    return (
      !!this.currentParticipant &&
      this.currentParticipant.status === 'REFUNDED' &&
      !this.isCurrentUserWinner()
    );
  }

  formatPrice(price: number): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600&q=80';
  }
}
