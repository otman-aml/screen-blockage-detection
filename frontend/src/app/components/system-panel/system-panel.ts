import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { SystemStateService } from '../../services/system-state.service';

type Health = 'up' | 'down' | 'unknown';

/**
 * Bloc "Etat du systeme" (conforme maquette) : etats REELS des services.
 * Backend/PostgreSQL : deduits du chargement REST reel. WebSocket : etat
 * de la connexion STOMP reelle. AI Service : ping health avec fallback
 * honnete "Non verifie" (jamais d'etat invente).
 */
@Component({
  selector: 'app-system-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <section class="card" aria-label="État du système">
      <h2>État du système</h2>
      <ul class="sys">
        <li>
          <span class="sys__name">Backend</span>
          <span class="sys__state" [class]="'sys__state--' + backendHealth()">
            <span class="sys__dot"></span>{{ label(backendHealth()) }}
          </span>
        </li>
        <li>
          <span class="sys__name">Base de données</span>
          <span class="sys__state" [class]="'sys__state--' + dbHealth()">
            <span class="sys__dot"></span>{{ label(dbHealth()) }}
          </span>
        </li>
        <li>
          <span class="sys__name">AI Service</span>
          <span class="sys__state" [class]="'sys__state--' + aiHealth()">
            <span class="sys__dot"></span>{{ label(aiHealth()) }}
          </span>
        </li>
        <li>
          <span class="sys__name">WebSocket</span>
          <span class="sys__state" [class]="'sys__state--' + (state.wsConnected() ? 'up' : 'down')">
            <span class="sys__dot"></span>{{ state.wsConnected() ? 'Connecté' : 'Déconnecté' }}
          </span>
        </li>
      </ul>
      <p class="sys__meta">
        Dernière analyse : {{ state.status()?.analyzedAt | date: 'HH:mm:ss' }}
      </p>
    </section>
  `,
  styles: `
    :host { display: block; }
    .card {
      background: var(--card-bg); border: 1px solid var(--border-color);
      border-radius: var(--radius); padding: 1rem 1.25rem; box-shadow: var(--shadow-sm);
    }
    h2 { margin: 0 0 0.75rem; font-size: 1rem; font-weight: 600; color: var(--navy); }
    .sys { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.55rem; }
    .sys li { display: flex; justify-content: space-between; align-items: center; }
    .sys__name { font-size: 0.85rem; color: var(--text-secondary); }
    .sys__state {
      display: inline-flex; align-items: center; gap: 0.4rem;
      font-size: 0.8rem; font-weight: 600;
    }
    .sys__dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
    .sys__state--up { color: var(--status-normal); }
    .sys__state--down { color: var(--status-critical); }
    .sys__state--unknown { color: var(--text-muted); }
    .sys__meta { margin: 0.75rem 0 0; font-size: 0.72rem; color: var(--text-muted); }
  `,
})
export class SystemPanel implements OnInit {
  protected readonly state = inject(SystemStateService);
  private readonly api = inject(ApiService);
  readonly backendHealth = signal<Health>('unknown');
  readonly dbHealth = signal<Health>('unknown');
  readonly aiHealth = signal<Health>('unknown');

  ngOnInit(): void {
    // Backend : un GET /api/status reussi prouve que le service repond.
    this.api.getStatus().subscribe({
      next: () => this.backendHealth.set('up'),
      error: () => this.backendHealth.set('down'),
    });
    // Base de donnees : /api/events est servie depuis PostgreSQL.
    this.api.getEvents().subscribe({
      next: () => this.dbHealth.set('up'),
      error: () => this.dbHealth.set('down'),
    });
    this.checkAi();
  }

  label(h: Health): string {
    return h === 'up' ? 'Opérationnel' : h === 'down' ? 'Indisponible' : 'Non vérifié';
  }

  private checkAi(): void {
    // mode 'no-cors' : la reponse est opaque mais prouve que le service
    // est joignable au niveau reseau (pas d'erreur CORS en console).
    // Echec reseau (service arrete) -> 'down'.
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 4000);
    fetch('http://localhost:8000/', { mode: 'no-cors', signal: controller.signal })
      .then((r) => {
        clearTimeout(timer);
        this.aiHealth.set(r.type === 'opaque' || r.ok ? 'up' : 'down');
      })
      .catch(() => {
        clearTimeout(timer);
        this.aiHealth.set('down');
      });
  }
}