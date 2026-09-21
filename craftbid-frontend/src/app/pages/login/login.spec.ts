import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Login } from './login';
import { AuthService, LoginResponse } from '../../services/auth';

describe('Login Component', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login', 'isLoggedIn', 'isAdmin']);
    authServiceSpy.isLoggedIn.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should validate empty identifier or password', () => {
    component.identifier = '';
    component.password = '';
    component.login();
    expect(component.errorMessage).toBe('Please enter your email or mobile number.');
    expect(authServiceSpy.login).not.toHaveBeenCalled();

    component.identifier = 'user@example.com';
    component.password = '';
    component.login();
    expect(component.errorMessage).toBe('Please enter your password.');
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should prevent duplicate login calls when request is in progress', () => {
    component.identifier = 'test@example.com';
    component.password = 'password123';
    component.loading = true;

    component.login();
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should handle successful login for customer role', () => {
    const mockResponse: LoginResponse = {
      token: 'jwt-mock-token',
      userId: 1,
      name: 'Test Buyer',
      email: 'buyer@example.com',
      role: 'CUSTOMER',
      sellerEnabled: false,
    };
    authServiceSpy.login.and.returnValue(of(mockResponse));

    component.identifier = 'buyer@example.com';
    component.password = 'secret';
    component.login();

    expect(component.loading).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('should handle authentication failure (HTTP 401) with generic message and clear password', () => {
    const errorResponse = new HttpErrorResponse({
      status: 401,
      statusText: 'Unauthorized',
      error: { message: 'Bad credentials' },
    });
    authServiceSpy.login.and.returnValue(throwError(() => errorResponse));

    component.identifier = 'buyer@example.com';
    component.password = 'wrongpassword';
    component.login();

    expect(component.errorMessage).toBe('Invalid email or password');
    expect(component.loading).toBeFalse();
    expect(component.identifier).toBe('buyer@example.com');
    expect(component.password).toBe('');
  });

  it('should handle unverified account error from backend', () => {
    const errorResponse = new HttpErrorResponse({
      status: 400,
      statusText: 'Bad Request',
      error: { message: 'Please verify your account before login' },
    });
    authServiceSpy.login.and.returnValue(throwError(() => errorResponse));

    component.identifier = 'buyer@example.com';
    component.password = 'mypassword';
    component.login();

    expect(component.errorMessage).toBe('Please verify your account before login.');
    expect(component.loading).toBeFalse();
    expect(component.password).toBe('');
  });

  it('should handle network failure or unexpected server error safely', () => {
    const networkError = new HttpErrorResponse({
      status: 0,
      statusText: 'Unknown Error',
    });
    authServiceSpy.login.and.returnValue(throwError(() => networkError));

    component.identifier = 'buyer@example.com';
    component.password = 'mypassword';
    component.login();

    expect(component.errorMessage).toBe('Unable to sign in right now. Please try again.');
    expect(component.loading).toBeFalse();
    expect(component.password).toBe('');
  });

  it('should handle HTTP 500 server error safely without technical details', () => {
    const serverError = new HttpErrorResponse({
      status: 500,
      statusText: 'Internal Server Error',
      error: 'java.lang.NullPointerException: internal database failure',
    });
    authServiceSpy.login.and.returnValue(throwError(() => serverError));

    component.identifier = 'buyer@example.com';
    component.password = 'mypassword';
    component.login();

    expect(component.errorMessage).toBe('Unable to sign in right now. Please try again.');
    expect(component.loading).toBeFalse();
    expect(component.password).toBe('');
  });
});
