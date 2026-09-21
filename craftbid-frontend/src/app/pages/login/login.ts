import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  identifier = '';
  password = '';
  showPassword = false;

  errorMessage = '';
  infoMessage = '';
  successMessage = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['sessionExpired'] === 'true') {
        this.infoMessage = 'Your session has expired. Please login again.';
      }
    });

    if (this.authService.isLoggedIn()) {
      if (this.authService.isAdmin()) {
        this.router.navigate(['/admin-dashboard']);
      } else {
        this.router.navigate(['/home']);
      }
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  login(): void {
    if (this.loading) {
      return;
    }

    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';

    const cleanIdentifier = (this.identifier || '').trim();

    if (!cleanIdentifier) {
      this.errorMessage = 'Please enter your email or mobile number.';
      return;
    }

    if (!this.password) {
      this.errorMessage = 'Please enter your password.';
      return;
    }

    this.loading = true;

    const loginData = {
      identifier: cleanIdentifier,
      password: this.password,
    };

    this.authService
      .login(loginData)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (response) => {
          if (response.role === 'ADMIN') {
            this.router.navigate(['/admin-dashboard']);
          } else {
            this.router.navigate(['/home']);
          }
        },

        error: (error: HttpErrorResponse | any) => {
          // Clear password field for security (keeping identifier populated)
          this.password = '';

          // Determine safe error message without technical data or credential leakage
          if (error && (error.status === 400 || error.status === 401 || error.status === 403)) {
            const rawMsg =
              typeof error.error === 'string'
                ? error.error
                : error.error?.message || error.error?.error || '';

            if (typeof rawMsg === 'string' && rawMsg.toLowerCase().includes('verify')) {
              this.errorMessage = 'Please verify your account before login.';
            } else {
              this.errorMessage = 'Invalid email or password';
            }
          } else if (error && error.status === 429) {
            this.errorMessage = 'Too many sign-in attempts. Please try again in a few moments.';
          } else {
            // Status 0 (network failure), 500/502/503/504 (server error), timeout, etc.
            this.errorMessage = 'Unable to sign in right now. Please try again.';
          }
          this.cdr.markForCheck();
        },
      });
  }
}
