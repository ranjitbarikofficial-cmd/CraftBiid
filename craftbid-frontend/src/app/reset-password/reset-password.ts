import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
})
export class ResetPassword {
  email = '';
  otp = '';

  newPassword = '';
  confirmPassword = '';
  showPassword = false;

  errorMessage = '';
  successMessage = '';
  loading = false;

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {
    this.email = sessionStorage.getItem('resetEmail') || '';
    this.otp = sessionStorage.getItem('resetOtp') || '';
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  resetPassword(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.newPassword) {
      this.errorMessage = 'Please enter your new password.';
      return;
    }

    if (this.newPassword.length < 6) {
      this.errorMessage = 'Password must contain at least 6 characters.';
      return;
    }

    if (!this.confirmPassword) {
      this.errorMessage = 'Please confirm your password.';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    if (!this.email || !this.otp) {
      this.errorMessage = 'Session expired. Please start the password reset again.';
      return;
    }

    this.loading = true;

    const data = {
      email: this.email,
      otp: this.otp,
      newPassword: this.newPassword,
    };

    this.http
      .post('/api/auth/reset-password', data, {
        responseType: 'text',
      })
      .subscribe({
        next: (response: string) => {
          console.log('Password reset:', response);
          this.loading = false;
          this.successMessage = 'Password updated successfully! Redirecting to login...';

          sessionStorage.removeItem('resetEmail');
          sessionStorage.removeItem('resetOtp');

          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 1500);
        },

        error: (error: any) => {
          console.error('Password reset error:', error);
          this.loading = false;

          let msg = 'Invalid OTP or password reset failed.';
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

          this.errorMessage = msg;
        },
      });
  }
}
