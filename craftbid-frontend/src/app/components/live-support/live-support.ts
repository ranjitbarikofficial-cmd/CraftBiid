import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupportService, SupportTicketItem } from '../../services/support.service';
import { AuthService, UserAuth } from '../../services/auth';
import { ToastService } from '../../services/toast.service';

interface ChatMessage {
  sender: 'bot' | 'user';
  text: string;
  time: string;
}

@Component({
  selector: 'app-live-support',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './live-support.html',
  styleUrl: './live-support.css',
})
export class LiveSupport implements OnInit {
  isOpen = false;
  activeTab: 'chat' | 'ticket' | 'faqs' = 'chat';

  currentUser: UserAuth | null = null;

  // Chat state
  userQuery = '';
  isTyping = false;
  messages: ChatMessage[] = [
    {
      sender: 'bot',
      text: 'Namaste! 🙏 Welcome to CraftBid 24/7 Customer Care. How can we help you today?',
      time: 'Just now',
    },
  ];

  // Ticket Form state
  ticketForm = {
    name: '',
    email: '',
    phone: '',
    category: 'AUCTION_BIDDING',
    subject: '',
    message: '',
  };
  submittingTicket = false;
  createdTicket: SupportTicketItem | null = null;

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
      }
    });
  }

  toggleWidget(): void {
    this.isOpen = !this.isOpen;
  }

  closeWidget(): void {
    this.isOpen = false;
  }

  sendChatMessage(): void {
    if (!this.userQuery.trim()) return;

    const query = this.userQuery.trim();
    this.messages.push({
      sender: 'user',
      text: query,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    });

    this.userQuery = '';
    this.isTyping = true;

    setTimeout(() => {
      this.isTyping = false;
      const reply = this.generateBotResponse(query);
      this.messages.push({
        sender: 'bot',
        text: reply,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      });
    }, 800);
  }

  sendQuickQuery(topic: string): void {
    this.userQuery = topic;
    this.sendChatMessage();
  }

  private generateBotResponse(q: string): string {
    const text = q.toLowerCase();
    if (text.includes('refund') || text.includes('money') || text.includes('deposit')) {
      return '🛡️ CraftBid Auto-Refund Policy: When an auction finishes, 100% of your deposits and differential bids are instantly credited back to your original UPI/bank account within seconds!';
    } else if (text.includes('bid') || text.includes('turn') || text.includes('minute') || text.includes('timer')) {
      return '⏱️ Live Turn Bidding: Up to 10 collectors join by paying the base deposit. Each new bid resets the 60-second countdown. If no one outbids you in 1 minute, you win the craft!';
    } else if (text.includes('ship') || text.includes('delivery') || text.includes('track') || text.includes('order')) {
      return '📦 Shipping & Dispatch: Winning buyers submit their shipping address immediately after turn completion. Artisans dispatch handcrafted items within 2-3 business days.';
    } else if (text.includes('artisan') || text.includes('seller') || text.includes('payout') || text.includes('fee')) {
      return '🎨 Artisan Payouts: Artisans keep 90% net earnings upon buyer address confirmation. CraftBid charges a flat 10% platform facilitation fee.';
    } else {
      return 'Thank you for reaching out! Our dedicated team is available 24/7 on WhatsApp & Phone at +91 90404 08690 or via email at support@craftbid.in. You can also submit a ticket from the "Submit Ticket" tab!';
    }
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
        this.createdTicket = ticket;
        this.toastService.success(`🎉 Ticket created! Ref: ${ticket.ticketRef}`);
      },
      error: (err) => {
        this.submittingTicket = false;
        console.error('Failed to submit ticket:', err);
        this.toastService.error('Failed to submit support ticket.');
      },
    });
  }

  resetTicketForm(): void {
    this.createdTicket = null;
    this.ticketForm.subject = '';
    this.ticketForm.message = '';
  }
}
