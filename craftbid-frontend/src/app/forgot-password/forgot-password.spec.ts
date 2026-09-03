import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPassword {
  email = '';

  message = '';
  errorMessage = '';

  loading = false;

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  sendOtp() {
    this.message = '';
    this.errorMessage = '';

    if (!this.email.trim()) {
      this.errorMessage = 'Please enter your email';
      return;
    }

    this.loading = true;

    this.http
      .post(
        'http://localhost:8081/api/auth/forgot-password',
        {
          email: this.email.trim(),
        },
        {
          responseType: 'text',
        },
      )
      .subscribe({
        next: (response) => {
          this.loading = false;

          this.message = response;

          localStorage.setItem('resetEmail', this.email.trim());

          this.router.navigate(['/reset-password']);
        },

        error: (error) => {
          this.loading = false;

          console.log(error);

          this.errorMessage = error.error || 'Unable to send OTP';
        },
      });
  }
}
