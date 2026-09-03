import { Routes } from '@angular/router';

import { Login } from './pages/login/login';
import { Register } from './register/register';
import { ForgotPassword } from './forgot-password/forgot-password';
import { VerifyOtp } from './verify-otp/verify-otp';
import { ResetPassword } from './reset-password/reset-password';
import { Home } from './pages/home/home';
import { RegisterOtp } from './register-otp/register-otp';
import { authGuard } from './guards/auth.guard';
import { artisanGuard } from './guards/artisan.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full',
  },
  {
    path: 'home',
    component: Home,
  },
  {
    path: 'login',
    component: Login,
  },
  {
    path: 'register',
    component: Register,
  },
  {
    path: 'register-otp',
    component: RegisterOtp,
  },
  {
    path: 'forgot-password',
    component: ForgotPassword,
  },
  {
    path: 'verify-otp',
    component: VerifyOtp,
  },
  {
    path: 'reset-password',
    component: ResetPassword,
  },
  {
    path: 'categories',
    loadComponent: () =>
      import('./pages/categories/categories').then((m) => m.Categories),
  },
  {
    path: 'search',
    loadComponent: () =>
      import('./pages/search/search').then((m) => m.Search),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/profile/profile').then((m) => m.Profile),
  },
  {
    path: 'auctions',
    loadComponent: () =>
      import('./pages/auctions/auctions').then((m) => m.Auctions),
  },
  {
    path: 'auctions/:id',
    loadComponent: () =>
      import('./pages/auction-details/auction-details').then((m) => m.AuctionDetails),
  },
  {
    path: 'my-bids',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/my-bids/my-bids').then((m) => m.MyBids),
  },
  {
    path: 'reels',
    loadComponent: () =>
      import('./pages/reels/reels').then((m) => m.Reels),
  },
  {
    path: 'crafts/:id',
    loadComponent: () =>
      import('./pages/craft-details/craft-details').then((m) => m.CraftDetails),
  },
  {
    path: 'artisan',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/artisan/artisan').then((m) => m.Artisan),
  },
  {
    path: 'artisan-dashboard',
    canActivate: [authGuard, artisanGuard],
    loadComponent: () =>
      import('./pages/artisan-dashboard/artisan-dashboard').then(
        (m) => m.ArtisanDashboard
      ),
  },
  {
    path: 'artisan/upload-craft',
    canActivate: [authGuard, artisanGuard],
    loadComponent: () =>
      import('./pages/upload-craft/upload-craft').then((m) => m.UploadCraft),
  },
  {
    path: 'upload-craft',
    redirectTo: 'artisan/upload-craft',
    pathMatch: 'full',
  },
  {
    path: 'admin-dashboard',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./pages/admin-dashboard/admin-dashboard').then(
        (m) => m.AdminDashboard
      ),
  },
  {
    path: 'admin',
    redirectTo: 'admin-dashboard',
    pathMatch: 'full',
  },
  {
    path: 'customer-service',
    loadComponent: () =>
      import('./pages/customer-service/customer-service').then(
        (m) => m.CustomerServicePage
      ),
  },
  {
    path: 'support',
    redirectTo: 'customer-service',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'home',
  },
];
