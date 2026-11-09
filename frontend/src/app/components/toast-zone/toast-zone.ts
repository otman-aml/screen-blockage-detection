import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast.service';

/**
 * Zone de notifications temps reel (Phase 7) : toats emis uniquement a
 * partir d'evenements reels (transitions WebSocket, resultats d'analyse).
 */
@Component({
  selector: 'app-toast-zone',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="toasts" aria-live="polite">
      @for (toast of toasts().slice(-4); track toast.id) {
        <div
          class="toast"
          [class.toast--warning]="toast.severity === 'warning'"
          [class.toast--critical]="toast.severity === 'critical'"
          [class.toast--success]="toast.severity === 'success'"
          role="alert"
        >
          <strong>{{ toast.title }}</strong>
          <span>{{ toast.message }}</span>
          <button type="button" class="toast__close" (click)="dismiss(toast.id)"
                  aria-label="Fermer la notification">×</button>
        </div>
      }
    </div>
  `,
  styles: `
    .toasts {
      position: fixed; right: 1rem; bottom: 1rem; z-index: 1000;
      display: flex; flex-direction: column; gap: 0.6rem; max-width: 340px;
    }
    .toast {
      background: #263238; color: #fff; border-radius: 8px;
      padding: 0.7rem 0.9rem; box-shadow: 0 4px 14px rgba(0,0,0,0.25);
      display: flex; flex-direction: column; gap: 0.15rem; font-size: 0.85rem;
      border-left: 5px solid #607d8b;
    }
    .toast strong { font-size: 0.8rem; }
    .toast--warning { border-left-color: #f9a825; }
    .toast--critical { border-left-color: #d32f2f; animation: shake 0.6s; }
    .toast--success { border-left-color: #43a047; }
    .toast__close {
      position: absolute; align-self: flex-end; margin-top: -1.4rem;
      background: none; border: none; color: #b0bec5; font-size: 1rem; cursor: pointer;
    }
    .toast__close:hover, .toast__close:focus-visible { color: #fff; }
    @keyframes shake { 25% { transform: translateX(-3px); } 75% { transform: translateX(3px); } }
  `,
})
export class ToastZone {
  private readonly toastService = inject(ToastService);
  readonly toasts = this.toastService.toasts;

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }
}
