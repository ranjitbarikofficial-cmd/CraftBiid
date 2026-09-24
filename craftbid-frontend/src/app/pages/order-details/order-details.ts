import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import { OrderService, OrderResponseDTO, SubmitAddressRequest, PackageDetailsRequest, CreateShipmentRequest, UpdateOrderStatusRequest, AddressDTO } from '../../services/order.service';
import { AuthService, UserAuth } from '../../services/auth';
import { ToastService } from '../../services/toast.service';
import { WebSocketService } from '../../services/websocket.service';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';
import { resolveMediaUrl } from '../../services/api-config';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './order-details.html',
  styleUrl: './order-details.css'
})
export class OrderDetails implements OnInit, OnDestroy {
  resolveMediaUrl = resolveMediaUrl;

  orderId: number | null = null;
  auctionId: number | null = null;
  orderNumber: string | null = null;
  order: OrderResponseDTO | null = null;
  currentUser: UserAuth | null = null;
  savedAddresses: AddressDTO[] = [];

  loading = true;
  submittingAddress = false;
  savingPackage = false;
  creatingShipment = false;
  updatingStatus = false;
  copiedAwb = false;

  // Modals
  isAddressModalOpen = false;
  isPackageModalOpen = false;
  isShipmentModalOpen = false;
  isStatusModalOpen = false;

  // Forms
  addressForm: SubmitAddressRequest = {
    addressId: undefined,
    fullName: '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    pincode: '',
    landmark: '',
    saveAsDefault: true
  };

  packageForm: PackageDetailsRequest = {
    weight: 1.0,
    length: 20.0,
    width: 15.0,
    height: 10.0,
    notes: ''
  };

  shipmentForm: CreateShipmentRequest = {
    courierName: 'Delhivery',
    trackingNumber: '',
    shippingCost: 0,
    trackingNotes: ''
  };

  statusForm: UpdateOrderStatusRequest = {
    status: '',
    trackingNotes: '',
    carrier: ''
  };

  private wsSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService,
    private authService: AuthService,
    private toastService: ToastService,
    private wsService: WebSocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      const auctionParam = params.get('auctionId');
      const numParam = params.get('orderNumber');

      if (idParam && !isNaN(Number(idParam))) {
        this.orderId = Number(idParam);
        this.fetchOrder();
      } else if (auctionParam && !isNaN(Number(auctionParam))) {
        this.auctionId = Number(auctionParam);
        this.fetchOrderByAuction();
      } else if (numParam) {
        this.orderNumber = numParam;
        this.fetchOrderByNumber();
      } else {
        this.fetchOrderFromRoute();
      }
    });

    this.loadSavedAddresses();
  }

  ngOnDestroy(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
  }

  fetchOrderFromRoute(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.orderId = Number(id);
      this.fetchOrder();
    }
  }

  fetchOrder(): void {
    if (!this.orderId) return;
    this.loading = true;
    this.orderService.getOrderById(this.orderId)
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (res) => {
          this.order = res;
          this.prefillForms(res);
          this.subscribeOrderUpdates(res.id);
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Failed to load order details');
        }
      });
  }

  fetchOrderByAuction(): void {
    if (!this.auctionId) return;
    this.loading = true;
    this.orderService.getOrderByAuctionId(this.auctionId)
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (res) => {
          this.order = res;
          this.orderId = res.id;
          this.prefillForms(res);
          this.subscribeOrderUpdates(res.id);
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Order not yet created for this auction');
        }
      });
  }

  fetchOrderByNumber(): void {
    if (!this.orderNumber) return;
    this.loading = true;
    this.orderService.getOrderByNumber(this.orderNumber)
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (res) => {
          this.order = res;
          this.orderId = res.id;
          this.prefillForms(res);
          this.subscribeOrderUpdates(res.id);
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Order not found');
        }
      });
  }

  loadSavedAddresses(): void {
    if (this.authService.isLoggedIn()) {
      this.orderService.getUserAddresses().subscribe({
        next: (addrs) => {
          this.savedAddresses = addrs;
          if (addrs.length > 0 && !this.addressForm.fullName) {
            const def = addrs.find(a => a.isDefault) || addrs[0];
            this.selectSavedAddress(def);
          }
        },
        error: () => {}
      });
    }
  }

  selectSavedAddress(addr: AddressDTO): void {
    this.addressForm.addressId = addr.id;
    this.addressForm.fullName = addr.fullName;
    this.addressForm.phone = addr.phone;
    this.addressForm.addressLine1 = addr.addressLine1;
    this.addressForm.addressLine2 = addr.addressLine2 || '';
    this.addressForm.city = addr.city;
    this.addressForm.state = addr.state;
    this.addressForm.pincode = addr.pincode;
    this.addressForm.landmark = addr.landmark || '';
  }

  prefillForms(order: OrderResponseDTO): void {
    if (order.fullName) {
      this.addressForm.fullName = order.fullName;
      this.addressForm.phone = order.phone || '';
      this.addressForm.addressLine1 = order.streetAddress || '';
      this.addressForm.city = order.city || '';
      this.addressForm.state = order.state || '';
      this.addressForm.pincode = order.pincode || '';
      this.addressForm.landmark = order.landmark || '';
    } else if (this.currentUser) {
      this.addressForm.fullName = this.currentUser.name || '';
      this.addressForm.phone = this.currentUser.phone || '';
    }

    if (order.packageWeight) {
      this.packageForm.weight = order.packageWeight;
      this.packageForm.length = order.packageLength || 20.0;
      this.packageForm.width = order.packageWidth || 15.0;
      this.packageForm.height = order.packageHeight || 10.0;
      this.packageForm.notes = order.trackingNotes || '';
    }

    if (order.courierName) {
      this.shipmentForm.courierName = order.courierName;
      this.shipmentForm.trackingNumber = order.trackingNumber || '';
    }
  }

  subscribeOrderUpdates(orderId: number): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
    // Listen for real-time WebSocket order events
    this.wsSubscription = this.wsService.subscribeToOrder(orderId).subscribe({
      next: (event: any) => {
        if (event && event.data) {
          this.order = event.data;
          this.toastService.info(`Order status updated: ${this.order?.statusDisplay}`);
          this.cdr.markForCheck();
        }
      },
      error: () => {}
    });
  }

  isBuyer(): boolean {
    if (!this.order || !this.currentUser) return false;
    return this.order.buyerId === this.currentUser.userId || this.order.buyerEmail === this.currentUser.email;
  }

  isArtisan(): boolean {
    if (!this.order || !this.currentUser) return false;
    return this.order.artisanId === this.currentUser.userId || this.order.artisanEmail === this.currentUser.email;
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  openAddressModal(): void {
    this.isAddressModalOpen = true;
  }

  closeAddressModal(): void {
    this.isAddressModalOpen = false;
  }

  submitAddress(): void {
    if (!this.orderId) return;
    if (!this.addressForm.fullName || !this.addressForm.phone || !this.addressForm.addressLine1 || !this.addressForm.city || !this.addressForm.state || !this.addressForm.pincode) {
      this.toastService.warning('Please fill in all required address fields.');
      return;
    }
    if (!/^[0-9]{6}$/.test(this.addressForm.pincode)) {
      this.toastService.warning('Please enter a valid 6-digit Indian PIN code.');
      return;
    }

    this.submittingAddress = true;
    this.orderService.submitDeliveryAddress(this.orderId, this.addressForm)
      .pipe(finalize(() => {
        this.submittingAddress = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (updated) => {
          this.order = updated;
          this.closeAddressModal();
          this.toastService.success('Delivery address confirmed! Artisan has been notified.');
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Failed to submit address');
        }
      });
  }

  openPackageModal(): void {
    this.isPackageModalOpen = true;
  }

  closePackageModal(): void {
    this.isPackageModalOpen = false;
  }

  submitPackageDetails(): void {
    if (!this.orderId) return;
    if (!this.packageForm.weight || this.packageForm.weight <= 0) {
      this.toastService.warning('Please enter a valid package weight in kg.');
      return;
    }

    this.savingPackage = true;
    this.orderService.updatePackageDetails(this.orderId, this.packageForm)
      .pipe(finalize(() => {
        this.savingPackage = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (updated) => {
          this.order = updated;
          this.closePackageModal();
          this.toastService.success('Package dimensions and weight saved.');
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Failed to save package details');
        }
      });
  }

  markReadyToShip(): void {
    if (!this.orderId) return;
    this.updatingStatus = true;
    this.orderService.markReadyToShip(this.orderId)
      .pipe(finalize(() => {
        this.updatingStatus = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (updated) => {
          this.order = updated;
          this.toastService.success('Order marked Ready for Shipping! Buyer notified.');
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Failed to update status');
        }
      });
  }

  openShipmentModal(): void {
    this.isShipmentModalOpen = true;
  }

  closeShipmentModal(): void {
    this.isShipmentModalOpen = false;
  }

  submitShipment(): void {
    if (!this.orderId) return;
    if (!this.shipmentForm.courierName || !this.shipmentForm.trackingNumber) {
      this.toastService.warning('Please enter courier name and AWB tracking number.');
      return;
    }

    this.creatingShipment = true;
    this.orderService.createShipment(this.orderId, this.shipmentForm)
      .pipe(finalize(() => {
        this.creatingShipment = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (updated) => {
          this.order = updated;
          this.closeShipmentModal();
          this.toastService.success('Shipment booked successfully! Tracking info shared with winner.');
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Failed to create shipment');
        }
      });
  }

  openStatusModal(status: string): void {
    this.statusForm.status = status;
    this.statusForm.carrier = this.order?.courierName || '';
    this.statusForm.trackingNotes = '';
    this.isStatusModalOpen = true;
  }

  closeStatusModal(): void {
    this.isStatusModalOpen = false;
  }

  submitStatusUpdate(): void {
    if (!this.orderId || !this.statusForm.status) return;

    this.updatingStatus = true;
    this.orderService.updateOrderStatus(this.orderId, this.statusForm)
      .pipe(finalize(() => {
        this.updatingStatus = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (updated) => {
          this.order = updated;
          this.closeStatusModal();
          this.toastService.success(`Order status advanced to ${updated.statusDisplay}`);
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Failed to update order status');
        }
      });
  }

  confirmDelivery(): void {
    if (!this.orderId) return;
    this.updatingStatus = true;
    this.orderService.updateOrderStatus(this.orderId, {
      status: 'COMPLETED',
      trackingNotes: 'Winner confirmed safe delivery and item inspection.'
    })
      .pipe(finalize(() => {
        this.updatingStatus = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (updated) => {
          this.order = updated;
          this.toastService.success('Thank you! Order marked as Completed.');
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Failed to confirm delivery');
        }
      });
  }

  copyTracking(): void {
    if (!this.order?.trackingNumber) return;
    navigator.clipboard.writeText(this.order.trackingNumber).then(() => {
      this.copiedAwb = true;
      this.toastService.success('AWB Tracking number copied to clipboard!');
      setTimeout(() => {
        this.copiedAwb = false;
        this.cdr.markForCheck();
      }, 2500);
    });
  }

  getStatusBadgeClass(status?: string): string {
    if (!status) return 'bg-gray-100 text-gray-800';
    switch (status) {
      case 'ADDRESS_REQUIRED': return 'bg-amber-100 text-amber-800 border border-amber-300';
      case 'ADDRESS_CONFIRMED': return 'bg-blue-100 text-blue-800 border border-blue-300';
      case 'SELLER_PREPARING': return 'bg-indigo-100 text-indigo-800 border border-indigo-300';
      case 'READY_TO_SHIP': return 'bg-purple-100 text-purple-800 border border-purple-300';
      case 'SHIPMENT_CREATED':
      case 'PICKUP_SCHEDULED':
      case 'PICKED_UP':
      case 'IN_TRANSIT':
      case 'OUT_FOR_DELIVERY': return 'bg-sky-100 text-sky-800 border border-sky-300';
      case 'DELIVERED':
      case 'COMPLETED': return 'bg-emerald-100 text-emerald-800 border border-emerald-300';
      case 'CANCELLED':
      case 'RTO':
      case 'DELIVERY_FAILED': return 'bg-rose-100 text-rose-800 border border-rose-300';
      default: return 'bg-gray-100 text-gray-800 border border-gray-300';
    }
  }
}
