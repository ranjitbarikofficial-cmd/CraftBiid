import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const artisanGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  if (authService.isSellerEnabled() || authService.isAdmin()) {
    return true;
  }

  router.navigate(['/artisan']);
  return false;
};
