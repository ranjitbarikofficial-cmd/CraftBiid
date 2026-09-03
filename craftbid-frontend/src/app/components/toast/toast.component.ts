import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from '../../services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-wrapper">
      @for (toast of toasts; track toast.id) {
        <div class="toast-item" [class]="toast.type" (click)="dismiss(toast.id)">
          <span class="icon">
            @if (toast.type === 'success') { ✅ }
            @if (toast.type === 'error') { ⚠️ }
            @if (toast.type === 'info') { ℹ️ }
            @if (toast.type === 'warning') { 🔔 }
          </span>
          <span class="message">{{ toast.text }}</span>
          <button class="close-btn" (click)="dismiss(toast.id); $event.stopPropagation()">✕</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-wrapper {
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 99999;
      display: flex;
      flex-direction: column;
      gap: 10px;
      max-width: 380px;
      pointer-events: none;
    }

    .toast-item {
      pointer-events: auto;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 18px;
      border-radius: 10px;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
      background: #ffffff;
      color: #0f172a;
      font-size: 0.9rem;
      font-weight: 500;
      border-left: 4px solid #3b82f6;
      animation: slideIn 0.25s ease-out;
      cursor: pointer;
    }

    @keyframes slideIn {
      from { transform: translateX(50px); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }

    .toast-item.success {
      border-left-color: #10b981;
      background: #f0fdf4;
      color: #166534;
    }

    .toast-item.error {
      border-left-color: #ef4444;
      background: #fef2f2;
      color: #991b1b;
    }

    .toast-item.warning {
      border-left-color: #f59e0b;
      background: #fffbeb;
      color: #92400e;
    }

    .toast-item.info {
      border-left-color: #3b82f6;
      background: #eff6ff;
      color: #1e40af;
    }

    .icon {
      font-size: 1.1rem;
    }

    .message {
      flex: 1;
      line-height: 1.4;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 0.9rem;
      cursor: pointer;
      color: inherit;
      opacity: 0.6;
      padding: 2px 6px;
    }

    .close-btn:hover {
      opacity: 1;
    }
  `]
})
export class ToastContainer implements OnInit {
  toasts: ToastMessage[] = [];

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.toastService.toasts$.subscribe((toasts) => {
      this.toasts = toasts;
    });
  }

  dismiss(id: number): void {
    this.toastService.remove(id);
  }
}
