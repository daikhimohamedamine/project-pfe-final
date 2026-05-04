import { Injectable, signal } from '@angular/core';

export type FeedbackType = 'success' | 'error' | 'info';
export interface FeedbackMessage {
  type: FeedbackType;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private get swal() {
    return (window as any).Swal;
  }

  success(text: string, title = 'Succès !') {
    this.swal?.fire({
      title: title,
      text: text,
      icon: 'success',
      timer: 3000,
      showConfirmButton: false,
      timerProgressBar: true,
      background: 'rgba(255, 255, 255, 0.95)',
      backdrop: `rgba(0, 0, 123, 0.1)`,
      customClass: {
        popup: 'glass-popup'
      }
    });
  }

  error(text: string, title = 'Erreur') {
    this.swal?.fire({
      title: title,
      text: text,
      icon: 'error',
      confirmButtonColor: '#3B82F6',
      background: 'rgba(255, 255, 255, 0.95)',
      customClass: {
        popup: 'glass-popup'
      }
    });
  }

  info(text: string, title = 'Information') {
    this.swal?.fire({
      title: title,
      text: text,
      icon: 'info',
      confirmButtonColor: '#3B82F6',
      background: 'rgba(255, 255, 255, 0.95)',
      customClass: {
        popup: 'glass-popup'
      }
    });
  }

  clear() {
    this.swal?.close();
  }

  // Backwards compatibility for templates using the signal
  message = signal<FeedbackMessage | null>(null);
}
