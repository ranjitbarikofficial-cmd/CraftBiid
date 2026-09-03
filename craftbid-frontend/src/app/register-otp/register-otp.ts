import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-register-otp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register-otp.html',
  styleUrl: './register-otp.css',
})
export class RegisterOtp implements OnInit {
  identifier = '';
  otp = '';
  password = '';

  errorMessage = '';
  successMessage = '';

  loading = false;
  resendLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private toastService: ToastService
  ) {
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state && nav.extras.state['password']) {
      this.password = nav.extras.state['password'];
    }
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.identifier = params['identifier'] || params['email'] || params['phone'] || '';
    });

    if (!this.identifier) {
      this.errorMessage = 'Registration identifier (email or phone) is missing.';
    }
  }

  verifyOtp(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const enteredOtp = this.otp.trim();

    if (!enteredOtp) {
      this.errorMessage = 'Please enter the 6-digit verification code.';
      return;
    }

    if (!/^\d{6}$/.test(enteredOtp)) {
      this.errorMessage = 'OTP must be 6 digits.';
      return;
    }

    if (!this.identifier) {
      this.errorMessage = 'Email or mobile number is missing.';
      return;
    }

    this.loading = true;

    this.authService.verifyRegistration(this.identifier, enteredOtp).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = 'Account verified successfully! Logging you in...';
        this.toastService.success('🎉 Welcome to CraftBid! Account verified.');

        // Seamless Auto-Login if password is in memory
        if (this.password) {
          this.authService
            .login({ identifier: this.identifier, password: this.password })
            .subscribe({
              next: () => {
                this.router.navigate(['/']);
              },
              error: () => {
                this.router.navigate(['/login']);
              },
            });
        } else {
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 1000);
        }
      },

      error: (error) => {
        console.error('OTP verification failed:', error);
        this.loading = false;
        this.errorMessage =
          error.error?.message ||
          error.error ||
          (typeof error.error === 'string' ? error.error : 'Invalid or expired OTP code.');
      },
    });
  }

  resendOtp(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.identifier) {
      this.errorMessage = 'Email or mobile number is missing.';
      return;
    }

    this.resendLoading = true;

    this.authService.resendVerificationOtp(this.identifier).subscribe({
      next: (response) => {
        this.resendLoading = false;
        this.successMessage = response || 'A new verification code has been sent.';
        this.toastService.info('📧 Verification code resent!');
      },

      error: (error) => {
        console.error('Resend OTP failed:', error);
        this.resendLoading = false;
        this.errorMessage =
          error.error?.message ||
          error.error ||
          (typeof error.error === 'string' ? error.error : 'Unable to resend OTP.');
      },
    });
  }
}
