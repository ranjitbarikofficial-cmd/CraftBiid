import { Component } from '@angular/core';

import { Topbar } from './topbar/topbar';
import { Navbar } from './navbar/navbar';
import { HomeContent } from './home-content/home-content';
import { Footer } from './footer/footer';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [Topbar, Navbar, HomeContent, Footer],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {}
