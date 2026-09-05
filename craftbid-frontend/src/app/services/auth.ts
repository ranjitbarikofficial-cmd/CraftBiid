import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface UserAuth {
  userId: number;
  name: string;
  email: string;
  phone?: string;
  role: 'CUSTOMER' | 'ADMIN' | string;
  sellerEnabled: boolean;
}

export interface LoginResponse {
  token: string;
  userId: number;
  name: string;
  email: string;
  phone?: string;
  role: string;
  sellerEnabled: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'https://craftbid.onrender.com/api/auth';
  private artisanUrl = 'https://craftbid.onrender.com/api/artisan';

  private currentUserSubject = new BehaviorSubject<UserAuth | null>(this.getStoredUser());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(data: { identifier: string; password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data).pipe(
      tap((response) => {
        this.saveAuth(response);
      }),
    );
  }

  sendAdminOtp(email: string = 'craftbid.official@gmail.com'): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/send-otp`, { email });
  }

  verifyAdminOtp(
    otp: string,
    email: string = 'craftbid.official@gmail.com',
  ): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/admin/verify-otp`, { email, otp }).pipe(
      tap((response) => {
        this.saveAuth(response);
      }),
    );
  }

  register(data: {
    name: string;
    email?: string;
    phone?: string;
    password: string;
  }): Observable<string> {
    return this.http.post(`${this.apiUrl}/register`, data, {
      responseType: 'text',
    });
  }

  verifyRegistration(identifier: string, otp: string): Observable<string> {
    return this.http.post(
      `${this.apiUrl}/verify-registration`,
      {
        identifier: identifier.trim(),
        otp: otp.trim(),
      },
      {
        responseType: 'text',
      },
    );
  }

  verifyEmail(email: string, otp: string): Observable<string> {
    return this.verifyRegistration(email, otp);
  }

  resendVerificationOtp(identifier: string): Observable<string> {
    return this.http.post(
      `${this.apiUrl}/resend-verification-otp`,
      {
        identifier: identifier.trim(),
      },
      {
        responseType: 'text',
      },
    );
  }

  forgotPassword(email: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email }, { responseType: 'text' });
  }

  resetPassword(data: { email: string; otp: string; newPassword: string }): Observable<string> {
    return this.http.post(`${this.apiUrl}/reset-password`, data, {
      responseType: 'text',
    });
  }

  enableSeller(data: { shopName: string; craftType: string; city: string }): Observable<string> {
    return this.http
      .post(`${this.artisanUrl}/enable`, data, {
        responseType: 'text',
      })
      .pipe(
        tap(() => {
          this.setSellerEnabled(true);
        }),
      );
  }

  saveAuth(response: LoginResponse): void {
    localStorage.setItem('token', response.token);
    const user: UserAuth = {
      userId: response.userId,
      name: response.name,
      email: response.email,
      role: response.role,
      sellerEnabled: response.sellerEnabled,
    };
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  saveToken(token: string): void {
    localStorage.setItem('token', token);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  private getStoredUser(): UserAuth | null {
    const raw = localStorage.getItem('currentUser');
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  getCurrentUser(): UserAuth | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isSellerEnabled(): boolean {
    const user = this.getCurrentUser();
    return !!user?.sellerEnabled;
  }

  getUserRole(): string | null {
    const user = this.getCurrentUser();
    return user ? user.role : null;
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  setSellerEnabled(enabled: boolean): void {
    const user = this.getCurrentUser();
    if (user) {
      user.sellerEnabled = enabled;
      localStorage.setItem('currentUser', JSON.stringify(user));
      this.currentUserSubject.next({ ...user });
    }
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }
}
