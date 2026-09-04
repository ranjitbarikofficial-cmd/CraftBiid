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
  // Login Mode: 'standard' or 'admin_otp'
  loginMode: 'standard' | 'admin_otp' = 'standard';

  // Standard Login Fields
  identifier = '';
  password = '';
  showPassword = false;

  // Admin OTP Login Fields
  adminEmail = 'craftbid.official@gmail.com';
  adminOtp = '';
  adminOtpSent = false;

  errorMessage = '';
  infoMessage = '';
  successMessage = '';
  loading = false;
  adminOtpLoading = false;

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
      if (params['mode'] === 'admin') {
        this.loginMode = 'admin_otp';
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

  setMode(mode: 'standard' | 'admin_otp'): void {
    this.loginMode = mode;
    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';
  }

  quickFill(role: 'artisan' | 'buyer' | 'admin' | 'ranjit'): void {
    if (role === 'artisan') {
      this.loginMode = 'standard';
      this.identifier = 'rb650196@gmail.com';
      this.password = 'buyer123';
    } else if (role === 'buyer') {
      this.loginMode = 'standard';
      this.identifier = 'buyer@craftbid.in';
      this.password = 'buyer123';
    } else if (role === 'ranjit') {
      this.loginMode = 'standard';
      this.identifier = 'ranjitbarik466@gmail.com';
      this.password = 'buyer123';
    } else if (role === 'admin') {
      this.loginMode = 'admin_otp';
      this.adminOtp = '123456';
    }
  }

  // ==========================================
  // STANDARD USER LOGIN
  // ==========================================
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
        console.log('Login successful:', response);
        this.loading = false;
        if (response.role === 'ADMIN') {
          this.router.navigate(['/admin-dashboard']);
        } else {
          this.router.navigate(['/home']);
        }
      },

      error: (error) => {
        console.error('Login failed:', error);
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

  // ==========================================
  // ADMIN OTP LOGIN (FIXED EMAIL)
  // ==========================================
  requestAdminOtp(): void {
    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';
    this.adminOtpLoading = true;

    this.authService.sendAdminOtp(this.adminEmail).subscribe({
      next: (response: any) => {
        console.log('Admin OTP response:', response);
        this.adminOtpLoading = false;
        this.adminOtpSent = true;
        this.adminOtp = '';
        this.successMessage = 'Security code dispatched to admin email. Please check your inbox.';
      },
      error: (error) => {
        console.error('Failed to send admin OTP:', error);
        this.adminOtpLoading = false;
        let msg = 'Failed to send OTP to admin email.';
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

  instantAdminLogin(): void {
    this.adminOtp = '123456';
    this.verifyAdminOtp();
  }

  verifyAdminOtp(): void {
    this.errorMessage = '';
    this.infoMessage = '';
    this.successMessage = '';

    if (!this.adminOtp.trim()) {
      this.errorMessage = 'Please enter the 6-digit OTP code.';
      return;
    }

    this.loading = true;

    this.authService.verifyAdminOtp(this.adminOtp.trim(), this.adminEmail).subscribe({
      next: (response) => {
        console.log('Admin OTP verification success:', response);
        this.loading = false;
        this.router.navigate(['/admin-dashboard']);
      },
      error: (error) => {
        console.error('Admin OTP verification failed:', error);
        this.loading = false;
        let msg = 'Invalid or expired OTP code.';
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
