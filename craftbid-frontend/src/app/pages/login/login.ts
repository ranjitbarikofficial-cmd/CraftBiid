import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
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
    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';

    if (!this.identifier.trim()) {
      this.errorMessage = 'Please enter your email or mobile number.';
      return;
    }

    if (!this.password) {
      this.errorMessage = 'Please enter your password.';
      return;
    }

    this.loading = true;

    const loginData = {
      identifier: this.identifier.trim(),
      password: this.password,
    };

    this.authService.login(loginData).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.role === 'ADMIN') {
          this.router.navigate(['/admin-dashboard']);
        } else {
          this.router.navigate(['/home']);
        }
      },

      error: (error) => {
        this.loading = false;

        let msg = 'Invalid email/mobile or password.';
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
      },
    });
  }
}
