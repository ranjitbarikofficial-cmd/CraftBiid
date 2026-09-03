import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar implements OnInit {
  isSeller = false;
  isAdmin = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(() => {
      this.checkStatus();
    });
  }

  checkStatus(): void {
    this.isSeller =
      this.authService.isLoggedIn() &&
      (this.authService.isSellerEnabled() || this.authService.isAdmin());
    this.isAdmin = this.authService.isLoggedIn() && this.authService.isAdmin();
  }
}
