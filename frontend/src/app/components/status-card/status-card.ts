import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Status } from '../../models/status.model';

/**
 * Carte d'affichage de l'etat metier courant.
 *
 * <p>Architecture: le BACKEND produit l'etat
 * {@link Status#status} (NORMAL/WARNING/CRITICAL est{d'autres indicateurs};
 * ce composant les affiche tels quels, sans aucune re-calcul metier.

 * <p>Le contour et/ou de la carte sont colores selon l'etat pour donner
 * un signal visuel immediat (mais la valeur affichee reste celle du backend).
 */
@Component({
  selector: 'app-status-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <section
      class="status-card"
      [class.status-normal]="status()?.status === 'NORMAL'"
      [class.status-warning]="status()?.status === 'WARNING'"
      [class.status-critical]="status()?.status === 'CRITICAL'"
      [class.status-empty]="!status()"
      aria-live="polite"
    >
      <header class="status-card__header">
        <h2>Etat de la bande transporteuse</h2>
        <span
          class="status-card__badge"
          [class.status-normal]="status()?.status === 'NORMAL'"
          [class.status-warning]="status()?.status === 'WARNING'"
          [class.status-critical]="status()?.status === 'CRITICAL'"
        >
          {{ status()?.status ?? 'En attente...' }}
        </span>
      </header>

      @if (status(); as s) {
        <div class="status-card__metrics">
          <div class="metric">
            <span class="metric__label">Blocage</span>
            <span class="metric__value">{{ s.blockagePercentage | number: '1.0-2' }} %</span>
          </div>
          <div class="metric">
            <span class="metric__label">Frames &gt; seuil</span>
            <span class="metric__value">{{ s.framesAboveThresholdPercentage | number: '1.0-2' }} %</span>
          </div>
          <div class="metric">
            <span class="metric__label">Roches surdimensionnées</span>
            <span class="metric__value">{{ s.oversizedRocks }}</span>
          </div>
          <div class="metric">
            <span class="metric__label">Échantillons</span>
            <span class="metric__value">{{ s.sampleCount }}</span>
          </div>
        </div>
        <footer class="status-card__meta">
          <span>
            Fenêtre début:
            {{ s.windowStart ? (s.windowStart | date: 'HH:mm:ss'): '—' }}
          </span>
          <span>
            Analyse:
            {{ s.analyzedAt ? (s.analyzedAt | date: 'HH:mm:ss'): '—' }}
          </span>
        </footer>
      } @else {
        <p class="status-card__waiting">En attente des donnees du backend...</p>
      }
    </section>
  `,
  styles: `
    :host {
      display: block;
    }
    .status-card {
      padding: 1.1rem 1.35rem;
      border-radius: var(--radius);
      border: 1px solid var(--border-color);
      background: var(--card-bg, #ffffff);
      box-shadow: var(--shadow-sm);
      border-left: 5px solid var(--status-color, #9e9e9e);
      transition: border-color 0.2s ease;
      min-height: 160px;
    }
    .status-card.status-empty {
      --status-color: #9e9e9e;
      border-left-color: var(--status-color);
    }
    .status-card.status-normal {
      --status-color: #2e7d32;
    }
    .status-card.status-warning {
      --status-color: #f9a825;
    }
    .status-card.status-critical {
      --status-color: #d32f2f;
      background: #fff3f3;
    }
    .status-card__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      margin-bottom: 0.75rem;
    }
    h2 {
      margin: 0;
      font-size: 1rem;
      font-weight: 600;
    }
    .status-card__badge {
      padding: 0.25rem 0.75rem;
      border-radius: 999px;
      font-weight: 700;
      font-size: 0.85rem;
      color: #fff;
      background: var(--status-color);
    }
    .status-card__metrics {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
      gap: 0.75rem;
    }
    .metric {
      background: rgba(0, 0, 0, 0.03);
      border-radius: 6px;
      padding: 0.5rem 0.75rem;
      display: flex;
      flex-direction: column;
    }
    .metric__label {
      font-size: 0.75rem;
      color: #666;
    }
    .metric__value {
      font-size: 1.25rem;
      font-weight: 700;
    }
    .status-card__meta {
      display: flex;
      gap: 0.75rem;
      font-size: 0.75rem;
      color: #777;
      margin-top: 0.75rem;
    }
    .status-card__meta span {
      background: #f4f4f4;
      padding: 0.15rem 0.5rem;
      border-radius: 4px;
    }
    .status-card__waiting {
      color: #888;
      font-style: italic;
    }
  `,
})
export class StatusCard {
  /** Etat metier courant, tel que produit par le backend. */
  readonly status = input<Status | null>(null);
}