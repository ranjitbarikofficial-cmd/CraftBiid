import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './verify-otp.html',
  styleUrl: './verify-otp.css',
})
export class VerifyOtp {
  otp = '';

  email = '';

  errorMessage = '';

  constructor(private router: Router) {
    this.email = sessionStorage.getItem('resetEmail') || '';
  }

  verifyOtp(): void {
    this.errorMessage = '';

    if (!this.otp.trim()) {
      this.errorMessage = 'Please enter the OTP.';
      return;
    }

    if (!/^\d{6}$/.test(this.otp.trim())) {
      this.errorMessage = 'OTP must be 6 digits.';
      return;
    }

    sessionStorage.setItem('resetOtp', this.otp.trim());

    this.router.navigate(['/reset-password']);
  }
}
