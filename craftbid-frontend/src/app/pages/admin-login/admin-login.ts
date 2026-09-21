import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService, LoginResponse } from '../../services/auth';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './admin-login.html',
  styleUrl: './admin-login.css',
})
export class AdminLogin implements OnInit, OnDestroy {
  authMethod: 'otp' | 'password' = 'otp';

  // OTP Login Fields
  adminEmail = 'craftbid.official@gmail.com';
  adminOtp = '';
  adminOtpSent = false;
  adminOtpLoading = false;

  // Password Login Fields
  passwordEmail = 'craftbid.official@gmail.com';
  adminPassword = '';
  showPassword = false;

  errorMessage = '';
  infoMessage = '';
  successMessage = '';
  loading = false;

  cooldownSeconds = 0;
  private cooldownTimer: any = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn() && this.authService.isAdmin()) {
      this.router.navigate(['/admin-dashboard']);
    }
  }

  ngOnDestroy(): void {
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer);
    }
  }

  setAuthMethod(method: 'otp' | 'password'): void {
    this.authMethod = method;
    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  // ==========================================
  // 1. REQUEST ADMIN OTP
  // ==========================================
  requestAdminOtp(): void {
    if (this.cooldownSeconds > 0) return;

    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';

    if (!this.adminEmail.trim()) {
      this.errorMessage = 'Please enter an authorized admin email address.';
      return;
    }

    this.adminOtpLoading = true;

    this.authService.sendAdminOtp(this.adminEmail.trim()).subscribe({
      next: (response: any) => {
        this.adminOtpLoading = false;
        this.adminOtpSent = true;
        this.adminOtp = '';
        this.successMessage =
          response?.message ||
          `6-digit security code dispatched to ${this.adminEmail}. Please check your inbox.`;
        this.startCooldown(60);
      },
      error: (error) => {
        this.adminOtpLoading = false;
        let msg = 'Failed to dispatch security code to admin email.';
        if (typeof error.error === 'string') {
          try {
            const parsed = JSON.parse(error.error);
            msg = parsed.message || parsed.error || error.error;
          } catch {
            msg = error.error;
          }
        } else if (error.error && typeof error.error === 'object') {
          msg = error.error.message || error.error.error || msg;
        }

        const waitMatch = msg.match(/wait\s+(\d+)\s+second/i);
        if (waitMatch && waitMatch[1]) {
          this.startCooldown(parseInt(waitMatch[1], 10));
        }

        this.errorMessage = msg;
      },
    });
  }

  // ==========================================
  // 2. VERIFY ADMIN OTP & REDIRECT
  // ==========================================
  verifyAdminOtp(): void {
    if (this.loading) return;

    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';

    const cleanOtp = (this.adminOtp || '').trim();
    if (!cleanOtp) {
      this.errorMessage = 'Please enter the 6-digit OTP security code.';
      return;
    }

    this.loading = true;

    this.authService
      .verifyAdminOtp(cleanOtp, (this.adminEmail || '').trim())
      .pipe(
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/admin-dashboard']);
        },
        error: (error) => {
          this.adminOtp = '';
          if (error && (error.status === 400 || error.status === 401 || error.status === 403)) {
            this.errorMessage = 'Invalid or expired OTP code. Please try again.';
          } else {
            this.errorMessage = 'Unable to verify security code right now. Please try again.';
          }
        },
      });
  }

  // ==========================================
  // 3. ADMIN PASSWORD AUTHENTICATION
  // ==========================================
  loginWithPassword(): void {
    if (this.loading) return;

    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';

    const cleanEmail = (this.passwordEmail || '').trim();
    if (!cleanEmail) {
      this.errorMessage = 'Please enter your admin email.';
      return;
    }

    if (!this.adminPassword) {
      this.errorMessage = 'Please enter your password.';
      return;
    }

    this.loading = true;

    this.authService
      .login({
        identifier: cleanEmail,
        password: this.adminPassword,
      })
      .pipe(
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: (response: LoginResponse) => {
          if (response.role === 'ADMIN') {
            this.router.navigate(['/admin-dashboard']);
          } else {
            this.router.navigate(['/home']);
          }
        },
        error: (error) => {
          this.adminPassword = '';
          if (error && (error.status === 400 || error.status === 401 || error.status === 403)) {
            this.errorMessage = 'Invalid email or password';
          } else if (error && error.status === 429) {
            this.errorMessage = 'Too many sign-in attempts. Please try again in a few moments.';
          } else {
            this.errorMessage = 'Unable to sign in right now. Please try again.';
          }
        },
      });
  }

  private startCooldown(seconds: number): void {
    this.cooldownSeconds = seconds;
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer);
    }
    this.cooldownTimer = setInterval(() => {
      this.cooldownSeconds--;
      if (this.cooldownSeconds <= 0) {
        clearInterval(this.cooldownTimer);
        this.cooldownTimer = null;
      }
    }, 1000);
  }
}
