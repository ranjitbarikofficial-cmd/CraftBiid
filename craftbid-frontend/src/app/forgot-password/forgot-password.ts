import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPassword {
  email = '';
  errorMessage = '';
  successMessage = '';
  loading = false;

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  sendOtp(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.email.trim()) {
      this.errorMessage = 'Please enter your registered email address.';
      return;
    }

    const email = this.email.trim().toLowerCase();
    this.loading = true;

    const data = {
      email: email,
    };

    this.http
      .post('/api/auth/forgot-password', data, { responseType: 'text' })
      .subscribe({
        next: (response: string) => {
          console.log('Backend response:', response);
          this.loading = false;
          sessionStorage.setItem('resetEmail', email);
          this.router.navigate(['/verify-otp']);
        },

        error: (error: any) => {
          console.error('Forgot password error:', error);
          this.loading = false;

          let msg = 'Failed to send OTP. Please check your email.';
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
