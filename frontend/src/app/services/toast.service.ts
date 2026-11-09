import { Injectable, signal } from '@angular/core';

/** Une notification temps reel affichee par le dashboard (Phase 7). */
export interface Toast {
  id: number;
  severity: 'info' | 'warning' | 'critical' | 'success';
  title: string;
  message: string;
}

/**
 * Service de notifications visuelles (Phase 7).
 * Les toasts sont emis a partir d'evenements REELS (messages WebSocket du
 * backend, resultats d'analyse) — jamais de contenu fabrique.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private readonly toastsSignal = signal<Toast[]>([]);

  /** Liste courante des toasts affiches. */
  readonly toasts = this.toastsSignal.asReadonly();

  show(severity: Toast['severity'], title: string, message: string, ttlMs = 6000): void {
    const toast: Toast = { id: this.nextId++, severity, title, message };
    this.toastsSignal.update((list) => [...list, toast]);
    setTimeout(() => this.dismiss(toast.id), ttlMs);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((list) => list.filter((t) => t.id !== id));
  }
}
