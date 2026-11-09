import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatsZone } from '../../components/stats-zone/stats-zone';
import { SystemStateService } from '../../services/system-state.service';

/**
 * Page /statistiques — statistiques REELLES du backend uniquement.
 * Repartition des transitions, volume d'analyses, tendance de blocage.
 */
@Component({
  selector: 'app-statistiques-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, StatsZone],
  template: `
    <h1 class="page-title">Statistiques</h1>

    <div class="stat-grid">
      <div class="stat-card">
        <span class="stat__value">{{ state.recent().length }}</span>
        <span class="stat__label">Analyses (historique récent)</span>
      </div>
      <div class="stat-card">
        <span class="stat__value">{{ state.events().length }}</span>
        <span class="stat__label">Transitions enregistrées</span>
      </div>
      <div class="stat-card stat-card--warning">
        <span class="stat__value">{{ countOf('WARNING') }}</span>
        <span class="stat__label">Passages en WARNING</span>
      </div>
      <div class="stat-card stat-card--critical">
        <span class="stat__value">{{ countOf('CRITICAL') }}</span>
        <span class="stat__label">Passages en CRITICAL</span>
      </div>
    </div>

    <app-stats-zone [analyses]="state.recent()"></app-stats-zone>
  `,
  styles: `
    :host { display: block; }
    .page-title {
      margin: 0 0 1rem; font-size: 1.3rem; font-weight: 800; color: var(--navy);
    }
    .stat-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
      gap: 1rem; margin-bottom: 1.25rem;
    }
    .stat-card {
      background: var(--card-bg, #fff); border: 1px solid var(--border-color);
      border-radius: var(--radius); padding: 1rem 1.1rem; box-shadow: var(--shadow-sm);
    }
    .stat-card--warning { border-left: 4px solid var(--status-warning); }
    .stat-card--critical { border-left: 4px solid var(--status-critical); }
    .stat__value { display: block; font-size: 1.7rem; font-weight: 800; color: var(--navy); }
    .stat__label { font-size: 0.75rem; color: var(--text-muted); }
  `,
})
export class StatistiquesPage {
  protected readonly state = inject(SystemStateService);

  /** Nombre d'analyses reelles avec le status donne (basé sur le blocage de l'image). */
  countOf(status: 'WARNING' | 'CRITICAL'): number {
    return this.state.recent().filter((a) => a.status === status).length;
  }
}