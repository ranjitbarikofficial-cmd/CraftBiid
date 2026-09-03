import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth';

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
    private router: Router
  ) {}

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  register(): void {
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

    const identifier = this.email.trim() ? this.email.trim() : this.phone.trim();

    const registerData = {
      name: this.name.trim(),
      email: this.email.trim() || undefined,
      phone: this.phone.trim() || undefined,
      password: this.password,
    };

    this.authService.register(registerData).subscribe({
      next: (response) => {
        console.log('Registration response:', response);
        this.loading = false;

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
        this.loading = false;

        let msg = 'Registration failed. Please check your details and try again.';
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

        this.errorMessage = msg;
        if (msg.toLowerCase().includes('already registered')) {
          this.isAlreadyRegistered = true;
        }
      },
    });
  }
}
