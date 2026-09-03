import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ToastMessage {
  id: number;
  text: string;
  type: 'success' | 'error' | 'info' | 'warning';
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  public toasts$ = this.toastsSubject.asObservable();
  private counter = 0;

  show(text: string, type: 'success' | 'error' | 'info' | 'warning' = 'info', durationMs = 4000): void {
    const id = ++this.counter;
    const current = this.toastsSubject.value;
    this.toastsSubject.next([...current, { id, text, type }]);

    setTimeout(() => {
      this.remove(id);
    }, durationMs);
  }

  success(text: string, durationMs = 4000): void {
    this.show(text, 'success', durationMs);
  }

  error(text: string, durationMs = 5000): void {
    this.show(text, 'error', durationMs);
  }

  info(text: string, durationMs = 4000): void {
    this.show(text, 'info', durationMs);
  }

  warning(text: string, durationMs = 4000): void {
    this.show(text, 'warning', durationMs);
  }

  remove(id: number): void {
    const current = this.toastsSubject.value;
    this.toastsSubject.next(current.filter((t) => t.id !== id));
  }
}
