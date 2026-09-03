import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastContainer } from './components/toast/toast.component';
import { LiveSupport } from './components/live-support/live-support';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastContainer, LiveSupport],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
