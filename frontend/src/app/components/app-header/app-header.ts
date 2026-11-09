import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Status } from '../../models/status.model';

/**
 * Header professionnel du dashboard.
 * Affiche le badge colore du statut REALTIME de l'analyse image
 * (NORMAL / WARNING / CRITICAL), l'indicateur WebSocket
 * et le badge DEMO MODE si actif.
 */
@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="hdr">
      <div class="hdr__left">
        <span class="hdr__title">OCP Screen Blockage Detection</span>
      </div>

      <div class="hdr__center">
        <span
          class="hdr__status"
          [class.hdr__status--normal]="imageStatus() === 'NORMAL'"
          [class.hdr__status--warning]="imageStatus() === 'WARNING'"
          [class.hdr__status--critical]="imageStatus() === 'CRITICAL'"
          [class.hdr__status--neutral]="!imageStatus()"
          role="status"
          [attr.aria-label]="'Analyse: ' + (imageStatus() ?? 'en attente')"
        >
          <span class="hdr__status-dot"></span>
          {{ imageStatus() || 'EN ATTENTE' }}
        </span>
      </div>

      <div class="hdr__right">
        @if (demoMode()) {
          <span class="hdr__demo" title="Cadence acceleree (1 img/s), pas la production (30 img/s)">
            <span class="material-symbols-outlined" aria-hidden="true">speed</span>
            DEMO MODE
          </span>
        }
        <span
          class="hdr__ws"
          [class.hdr__ws--ok]="wsConnected()"
          [attr.aria-label]="wsConnected() ? 'WebSocket connecte' : 'WebSocket deconnecte'"
        >
          <span class="material-symbols-outlined" aria-hidden="true">
            {{ wsConnected() ? 'wifi' : 'wifi_off' }}
          </span>
          {{ wsConnected() ? 'Connecté' : 'Déconnecté' }}
        </span>
      </div>
    </header>
  `,
  styles: `
    :host { display: block; }
    .hdr {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 0 1.5rem;
      height: 56px;
      background: var(--surface);
      border-bottom: 1px solid var(--border-color);
      box-shadow: var(--shadow-sm);
    }
    .hdr__left { display: flex; align-items: center; }
    .hdr__title {
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--navy);
      letter-spacing: 0.01em;
    }
    .hdr__center { flex: 1; display: flex; justify-content: center; }
    .hdr__status {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 700;
      letter-spacing: 0.04em;
      padding: 0.4rem 1.1rem;
      border-radius: 999px;
      font-size: 0.82rem;
      text-transform: uppercase;
      background: #e2e8f0;
      color: var(--text-secondary);
    }
    .hdr__status--neutral { background: #f1f5f9; color: #64748b; }
    .hdr__status--normal { background: #dcfce7; color: #166534; }
    .hdr__status--warning { background: #ffedd5; color: #9a3412; }
    .hdr__status--critical { background: #fee2e2; color: #991b1b; animation: pulse 1.6s infinite; }
    .hdr__status-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: currentColor;
    }
    .hdr__right { display: flex; align-items: center; gap: 0.75rem; }
    .hdr__ws {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.78rem;
      font-weight: 500;
      color: var(--text-muted);
    }
    .hdr__ws .material-symbols-outlined { font-size: 18px; }
    .hdr__ws--ok { color: #166534; }
    .hdr__ws--ok .material-symbols-outlined { color: #16a34a; }
    .hdr__demo {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      background: #fef3c7;
      color: #92400e;
      font-weight: 700;
      font-size: 0.72rem;
      padding: 0.35rem 0.7rem;
      border-radius: 6px;
      letter-spacing: 0.02em;
    }
    .hdr__demo .material-symbols-outlined { font-size: 16px; }
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.7; }
    }
    @media (max-width: 720px) {
      .hdr { flex-wrap: wrap; height: auto; padding: 0.5rem 1rem; }
      .hdr__center { order: 3; flex-basis: 100%; justify-content: flex-start; }
    }
  `,
})
export class AppHeader {
  readonly status = input<Status | null>(null);
  readonly imageStatus = input<string | null>(null);
  readonly wsConnected = input(false);
  readonly demoMode = input(false);
}
