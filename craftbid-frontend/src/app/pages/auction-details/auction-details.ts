import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import {
  AuctionService,
  AuctionItem,
  BidItem,
  AuctionParticipantItem,
  AuctionOrderItem,
  SubmitAddressPayload,
} from '../../services/auction.service';
import { PaymentService } from '../../services/payment.service';
import { WebSocketService, WebSocketEvent } from '../../services/websocket.service';
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
  participationTimeLeft = '';
  timerInterval: any = null;
  pollingInterval: any = null;
  private hasExpiredTriggered = false;
  private isRefreshing = false;
  private wsSubscription: Subscription | null = null;
  private routeSub: Subscription | null = null;
  private currentWsTopic: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auctionService: AuctionService,
    private paymentService: PaymentService,
    private wsService: WebSocketService,
    private authService: AuthService,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.routeSub = this.route.params.subscribe((params) => {
      const id = Number(params['id']);
      if (id) {
        this.loadAuction(id);
        this.setupWebSocket(id);
      }
    });

    this.timerInterval = setInterval(() => {
      this.updateTurnTimer();
      this.updateParticipationCountdown();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
    if (this.routeSub) {
      this.routeSub.unsubscribe();
      this.routeSub = null;
    }
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
      this.wsSubscription = null;
    }
    if (this.currentWsTopic) {
      this.wsService.unsubscribe(this.currentWsTopic);
      this.currentWsTopic = null;
    }
  }

  setupWebSocket(auctionId: number): void {
    const newTopic = `/topic/auctions/${auctionId}`;
    if (this.currentWsTopic && this.currentWsTopic !== newTopic) {
      this.wsService.unsubscribe(this.currentWsTopic);
    }
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
      this.wsSubscription = null;
    }
    this.currentWsTopic = newTopic;
    this.wsSubscription = this.wsService.subscribeToAuction(auctionId).subscribe({
      next: (event: WebSocketEvent) => {
        this.handleWebSocketEvent(event);
      },
    });
  }

  private handleWebSocketEvent(event: WebSocketEvent): void {
    if (!event) return;

    if (event.eventType === 'auction:joined') {
      if (event.data?.currentParticipants !== undefined && this.auction) {
        this.auction.currentParticipantsCount = event.data.currentParticipants;
      }
      if (event.data?.firstDepositPaidAt && this.auction) {
        this.auction.firstDepositPaidAt = event.data.firstDepositPaidAt;
      }
      if (event.data?.participationDeadline && this.auction) {
        this.auction.participationDeadline = event.data.participationDeadline;
      }
      if (this.auction) {
        this.loadParticipants(this.auction.id);
      }
    } else if (event.eventType === 'auction:participation_started') {
      if (this.auction) {
        if (event.data?.participationDeadline) {
          this.auction.participationDeadline = event.data.participationDeadline;
        }
        if (event.data?.firstDepositPaidAt) {
          this.auction.firstDepositPaidAt = event.data.firstDepositPaidAt;
        }
        this.toastService.info('🏺 The 24-hour participation window has officially started!');
        this.refreshData(this.auction.id);
      }
    } else if (event.eventType === 'auction:started') {
      if (this.auction) {
        this.auction.liveTurnActive = true;
        this.auction.status = 'ACTIVE';
        if (event.data?.turnDeadline) {
          this.auction.turnDeadline = event.data.turnDeadline;
        }
        this.hasExpiredTriggered = false;
        this.toastService.info('⚡ Live 1-Minute Auction has officially started! Place your bids.');
      }
    } else if (event.eventType === 'auction:bid') {
      if (this.auction) {
        this.auction.currentHighestBid = event.data?.amount || this.auction.currentHighestBid;
        this.auction.totalBids = event.data?.totalBids || (this.auction.totalBids + 1);
        if (event.data?.turnDeadline) {
          this.auction.turnDeadline = event.data.turnDeadline;
        }
        this.hasExpiredTriggered = false;
        this.calculateMinNextBid();
        this.loadBids(this.auction.id);
        this.loadParticipants(this.auction.id);
      }
    } else if (event.eventType === 'auction:ended' || event.eventType === 'auction:winner') {
      if (this.auction) {
        this.auction.status = 'ENDED';
        this.auction.liveTurnActive = false;
        this.refreshData(this.auction.id);
        this.loadOrder(this.auction.id);
      }
    }
    this.cdr.markForCheck();
  }

  loadAuction(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.auctionService.getAuctionById(id).pipe(
      finalize(() => {
        this.loading = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (data) => {
        this.auction = data;
        this.calculateMinNextBid();
        this.loadBids(id);
        this.loadParticipants(id);
        if (this.auction.status === 'ENDED') {
          this.loadOrder(id);
        }

        if (!this.pollingInterval) {
          // Low-frequency fallback refresh (every 15 seconds) to prevent network storms
          this.pollingInterval = setInterval(() => {
            if (!this.isJoinModalOpen && !this.isAddressModalOpen) {
              this.refreshData(id);
            }
          }, 15000);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage = 'Auction not found.';
        this.cdr.markForCheck();
      },
    });
  }

  loadBids(auctionId: number): void {
    this.auctionService.getAuctionBids(auctionId).subscribe({
      next: (data) => {
        this.bids = data || [];
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  loadParticipants(auctionId: number): void {
    this.auctionService.getParticipants(auctionId).subscribe({
      next: (data) => {
        this.participants = data || [];
        if (this.currentUser) {
          this.currentParticipant =
            this.participants.find(
              (p) => (p.user && p.user.id === this.currentUser!.userId) || (p.userId === this.currentUser!.userId),
            ) || null;
        }
        this.updateDifferentialToPay();
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  loadOrder(auctionId: number): void {
    if (this.currentUser && (this.auction?.status === 'ENDED' || this.isCurrentUserWinner())) {
      this.auctionService.getAuctionOrder(auctionId).subscribe({
        next: (order) => {
          this.auctionOrder = order || null;
          this.cdr.markForCheck();
        },
        error: () => {
          this.auctionOrder = null;
          this.cdr.markForCheck();
        },
      });
    }
  }

  refreshData(auctionId: number): void {
    if (this.isRefreshing) return;
    this.isRefreshing = true;

    this.auctionService.getAuctionById(auctionId).pipe(
      finalize(() => {
        this.isRefreshing = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (updatedAuction) => {
        this.auction = updatedAuction;
        this.calculateMinNextBid();
        this.loadBids(auctionId);
        this.loadParticipants(auctionId);
        if (this.auction.status === 'ENDED') {
          this.loadOrder(auctionId);
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.cdr.markForCheck();
      }
    });
  }

  updateTurnTimer(): void {
    if (!this.auction || this.auction.status === 'ENDED' || this.auction.status === 'CANCELLED') {
      this.secondsRemainingInTurn = 0;
      this.cdr.markForCheck();
      return;
    }

    if (!this.auction.liveTurnActive || !this.auction.turnDeadline) {
      this.secondsRemainingInTurn = 60;
      this.hasExpiredTriggered = false;
      this.cdr.markForCheck();
      return;
    }

    const deadline = new Date(this.auction.turnDeadline).getTime();
    const now = Date.now();
    const diffSeconds = Math.max(0, Math.floor((deadline - now) / 1000));
    this.secondsRemainingInTurn = diffSeconds;

    // Trigger refresh strictly once on transition to 0, avoiding repeated 1-second loops
    if (diffSeconds === 0 && !this.hasExpiredTriggered && (this.auction.status === 'ACTIVE' || this.auction.status === 'LIVE')) {
      this.hasExpiredTriggered = true;
      this.refreshData(this.auction.id);
    } else if (diffSeconds > 0) {
      this.hasExpiredTriggered = false;
    }

    this.cdr.markForCheck();
  }

  isWaitingForFirstDeposit(): boolean {
    return (
      !this.auction?.liveTurnActive &&
      this.auction?.status !== 'ENDED' &&
      this.auction?.status !== 'CANCELLED' &&
      (!this.auction?.participationDeadline || (this.auction?.currentParticipantsCount || 0) === 0)
    );
  }

  isParticipationWindowActive(): boolean {
    return (
      !this.auction?.liveTurnActive &&
      this.auction?.status !== 'ENDED' &&
      this.auction?.status !== 'CANCELLED' &&
      !!this.auction?.participationDeadline &&
      (this.auction?.currentParticipantsCount || 0) > 0
    );
  }

  updateParticipationCountdown(): void {
    if (
      !this.auction ||
      !this.auction.participationDeadline ||
      (this.auction.currentParticipantsCount || 0) === 0
    ) {
      this.participationTimeLeft = '';
      this.cdr.markForCheck();
      return;
    }

    const deadline = new Date(this.auction.participationDeadline).getTime();
    const now = Date.now();
    const diffMs = deadline - now;

    if (diffMs <= 0) {
      this.participationTimeLeft = 'Participation Window Closed';
      this.cdr.markForCheck();
      return;
    }

    const hours = Math.floor(diffMs / (1000 * 60 * 60));
    const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diffMs % (1000 * 60)) / 1000);

    this.participationTimeLeft = `${hours}h ${minutes}m ${seconds}s`;
    this.cdr.markForCheck();
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
    this.cdr.markForCheck();
  }

  addBidIncrement(increment: number): void {
    if (!this.auction) return;
    const base = this.auction.totalBids === 0 ? this.auction.startingPrice : this.auction.currentHighestBid;
    this.bidAmount = base + increment;
    this.updateDifferentialToPay();
    this.cdr.markForCheck();
  }

  onBidAmountChange(): void {
    this.updateDifferentialToPay();
    this.cdr.markForCheck();
  }

  updateDifferentialToPay(): void {
    if (!this.bidAmount) {
      this.differentialToPay = 0;
      return;
    }
    const alreadyPaid = this.currentParticipant ? this.currentParticipant.totalAmountPaid : 0;
    this.differentialToPay = Math.max(0, this.bidAmount - alreadyPaid);
    this.cdr.markForCheck();
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

    if (this.isCurrentUserSeller()) {
      this.toastService.info('👨‍🎨 You created this craft. Artisans cannot bid or join their own auctions.');
      return;
    }

    if (this.currentParticipant) {
      this.toastService.info('You have already joined this auction room.');
      return;
    }

    this.isJoinModalOpen = true;
    this.cdr.markForCheck();
  }

  closeJoinModal(): void {
    this.isJoinModalOpen = false;
    this.joining = false;
    this.cdr.markForCheck();
  }

  confirmJoinAuction(): void {
    if (!this.auction) return;
    if (this.joining) return;

    if (!this.authService.isLoggedIn()) {
      this.toastService.warning('Please login before joining the auction');
      this.router.navigate(['/login']);
      return;
    }

    if (this.currentParticipant) {
      this.toastService.info('You are already an active participant in this auction.');
      this.closeJoinModal();
      return;
    }

    this.joining = true;
    this.cdr.markForCheck();

    const auctionId = this.auction.id;
    const craftId = this.auction.craft?.id;
    const startingPrice = this.auction.startingPrice;
    const method = this.selectedPaymentMethod || 'UPI';

    if (method === 'UPI' || method === 'NETBANKING') {
      // 1. Direct deposit participation flow with selected payment method
      this.auctionService
        .joinAuctionWithDeposit(auctionId, method)
        .pipe(
          finalize(() => {
            this.joining = false;
            this.cdr.markForCheck();
          })
        )
        .subscribe({
          next: (participant) => {
            this.currentParticipant = participant;
            this.closeJoinModal();
            this.toastService.success(
              `🎉 Base deposit of ${this.formatPrice(startingPrice)} confirmed! You have joined Auction #${auctionId}.`
            );
            this.refreshData(auctionId);
          },
          error: (err) => {
            if (err.status === 409 || err.error?.code === 'ALREADY_JOINED') {
              this.toastService.info('You have already joined this auction.');
              this.closeJoinModal();
              this.refreshData(auctionId);
              return;
            }
            if (err.status === 403) {
              const msg = err.error?.message || 'Artisans cannot join their own auctions.';
              this.toastService.warning(msg);
              this.closeJoinModal();
              return;
            }
            const msg = err.error?.message || 'Failed to complete auction deposit. Please try again.';
            this.toastService.error(msg);
          },
        });
    } else {
      // 2. Gateway checkout flow (Card / Razorpay)
      this.paymentService
        .createRazorpayOrder(startingPrice, auctionId, craftId, 'PARTICIPATION')
        .pipe(
          finalize(() => {
            // If createRazorpayOrder itself fails, finalize ensures joining reset
            this.cdr.markForCheck();
          })
        )
        .subscribe({
          next: (orderRes) => {
            this.paymentService.openRazorpayCheckout(
              orderRes,
              (verifyPayload) => {
                // Payment captured on gateway -> verify signature on backend
                this.paymentService
                  .verifyRazorpayPayment(verifyPayload)
                  .pipe(
                    finalize(() => {
                      this.joining = false;
                      this.cdr.markForCheck();
                    })
                  )
                  .subscribe({
                    next: () => {
                      this.closeJoinModal();
                      this.toastService.success(`🎉 Payment verified! You joined Auction #${auctionId}.`);
                      this.refreshData(auctionId);
                    },
                    error: (err) => {
                      const msg = err.error?.message || err.error || 'Payment signature verification failed.';
                      this.toastService.error(msg);
                    },
                  });
              },
              () => {
                // Modal dismissed / cancelled
                this.joining = false;
                this.cdr.markForCheck();
                this.toastService.info('Payment window closed.');
              },
              (err) => {
                // Gateway error
                this.joining = false;
                this.cdr.markForCheck();
                this.toastService.error(err?.description || err?.message || 'Payment gateway error.');
              }
            );
          },
          error: (err) => {
            this.joining = false;
            this.cdr.markForCheck();
            const msg = err.error?.message || err.error || 'Failed to create payment order.';
            this.toastService.error(msg);
          },
        });
    }
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
    if (this.bidding) return;

    this.bidding = true;
    this.cdr.markForCheck();

    this.auctionService
      .placeDifferentialBid(this.auction.id, this.bidAmount)
      .pipe(
        finalize(() => {
          this.bidding = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.toastService.success(
            `🎉 Differential of ${this.formatPrice(this.differentialToPay)} paid! Highest Bid set to ${this.formatPrice(this.bidAmount!)}`
          );
          this.refreshData(this.auction!.id);
        },
        error: (err) => {
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
    this.cdr.markForCheck();
  }

  closeAddressModal(): void {
    this.isAddressModalOpen = false;
    this.submittingAddress = false;
    this.cdr.markForCheck();
  }

  submitAddress(): void {
    if (!this.auction) return;
    if (this.submittingAddress) return;

    if (
      !this.addressForm.fullName ||
      !this.addressForm.streetAddress ||
      !this.addressForm.city ||
      !this.addressForm.pincode ||
      !this.addressForm.phone
    ) {
      this.toastService.error('Please fill in all delivery address fields');
      return;
    }

    this.submittingAddress = true;
    this.cdr.markForCheck();

    this.auctionService
      .submitDeliveryAddress(this.auction.id, this.addressForm)
      .pipe(
        finalize(() => {
          this.submittingAddress = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (order) => {
          this.auctionOrder = order;
          this.closeAddressModal();
          this.toastService.success('📦 Delivery address confirmed! Artisan notified for shipping.');
        },
        error: (err) => {
          const msg = err.error?.message || err.error || 'Failed to submit address.';
          this.toastService.error(msg);
        },
      });
  }

  isCurrentUserSeller(): boolean {
    if (!this.auction || !this.currentUser) return false;
    const sellerId = this.auction.seller?.id || this.auction.craft?.seller?.id;
    return sellerId === this.currentUser.userId;
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

  formatPrice(price?: number | null): string {
    return '₹' + (price || 0).toLocaleString('en-IN');
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600&q=80';
  }
}

