import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  name = '';
  email = '';
  phone = '';
  password = '';
  showPassword = false;

  errorMessage = '';
  isAlreadyRegistered = false;
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  register(): void {
    if (this.loading) {
      return;
    }

    this.errorMessage = '';
    this.isAlreadyRegistered = false;

    if (!this.name.trim()) {
      this.errorMessage = 'Please enter your full name.';
      return;
    }

    if (!this.email.trim() && !this.phone.trim()) {
      this.errorMessage = 'Please provide an email address or mobile number.';
      return;
    }

    if (!this.password || this.password.length < 6) {
      this.errorMessage = 'Password must be at least 6 characters long.';
      return;
    }

    this.loading = true;
    this.cdr.markForCheck();

    const identifier = this.email.trim() ? this.email.trim() : this.phone.trim();

    const registerData = {
      name: this.name.trim(),
      email: this.email.trim() || undefined,
      phone: this.phone.trim() || undefined,
      password: this.password,
    };

    this.authService
      .register(registerData)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          // Navigate to OTP verification with the identifier and password state for seamless auto-login
          this.router.navigate(['/register-otp'], {
            queryParams: {
              identifier: identifier,
              email: this.email.trim() || undefined,
              phone: this.phone.trim() || undefined,
            },
            state: {
              password: this.password,
            },
          });
        },

        error: (error) => {
          console.error('Registration failed:', error);

          let msg = 'Registration failed. Please check your details and try again.';

          if (error.status === 0) {
            msg = 'Unable to connect to CraftBid. Please check your internet connection and try again.';
          } else if (error.status >= 500) {
            msg = 'Something went wrong. Please try again later.';
          } else {
            // Check structured message from backend
            if (typeof error.error === 'string') {
              try {
                const parsed = JSON.parse(error.error);
                msg = parsed.message || parsed.error || error.error;
              } catch {
                msg = error.error;
              }
            } else if (error.error && typeof error.error === 'object') {
              msg = error.error.message || error.error.error || msg;
            } else if (error.message) {
              msg = error.message;
            }
          }

          this.errorMessage = msg;

          const lowerMsg = (msg || '').toLowerCase();
          if (
            lowerMsg.includes('already registered') ||
            lowerMsg.includes('already exists') ||
            error.status === 409
          ) {
            this.isAlreadyRegistered = true;
            if (!this.errorMessage.includes('Please login')) {
              this.errorMessage = 'Email is already registered. Please login.';
            }
          }

          this.cdr.markForCheck();
        },
      });
  }
}
