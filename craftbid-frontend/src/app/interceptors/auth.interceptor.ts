import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');

  let authReq = req;

  // Add Authorization header if a valid token exists and header isn't already present
  if (
    token &&
    token !== 'null' &&
    token !== 'undefined' &&
    token.trim().length > 0 &&
    !req.headers.has('Authorization')
  ) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token.trim()}`,
      },
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // If unauthorized (and not already on login/register endpoints), clear session and redirect
      if (error.status === 401 && !req.url.includes('/api/auth/login')) {
        localStorage.removeItem('token');
        localStorage.removeItem('currentUser');
        router.navigate(['/login'], {
          queryParams: { sessionExpired: 'true' },
        });
      }
      return throwError(() => error);
    })
  );
};
