import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SystemStateService } from '../../services/system-state.service';
import { KpiZone } from '../kpi-zone/kpi-zone';
import { AnalysisView } from '../analysis-view/analysis-view';
import { AnalysisHistory } from '../analysis-history/analysis-history';
import { SystemPanel } from '../system-panel/system-panel';

/**
 * Page /dashboard — vue SYNTHETIQUE temps reel (compacte) :
 * KPI + etat de la bande + analyse recente + derniers evenements.
 * Le graphique et l'historique complet sont sur les pages dediees.
 */
@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DecimalPipe, DatePipe, KpiZone, AnalysisView, AnalysisHistory, SystemPanel],
  template: `
    <app-kpi-zone
      [status]="state.status()"
      [events]="state.events()"
      [analyses]="state.recent()"
      [lastAnalysis]="state.recent()[0] ?? null"
    ></app-kpi-zone>

    <section class="card" aria-label="État de la bande transporteuse">
      <h2>État de la bande transporteuse</h2>
      <div class="belt">
        <div class="belt__item">
          <span class="belt__value">{{ (state.status()?.blockagePercentage ?? 0) | number: '1.0-2' }} %</span>
          <span class="belt__label">Blocage</span>
        </div>
        <div class="belt__item">
          <span class="belt__value">{{ (state.status()?.framesAboveThresholdPercentage ?? 0) | number: '1.0-1' }} %</span>
          <span class="belt__label">Frames > seuil</span>
        </div>
        <div class="belt__item">
          <span class="belt__value">{{ state.status()?.oversizedRocks ?? '—' }}</span>
          <span class="belt__label">Roches surdimensionnées</span>
        </div>
        <div class="belt__item">
          <span class="belt__value">{{ state.status()?.sampleCount ?? '—' }}</span>
          <span class="belt__label">Échantillons</span>
        </div>
      </div>
    </section>

    <app-system-panel></app-system-panel>

    <app-analysis-view
      [latestAnalysis]="state.latestAnalysis()"
      [latestImageUrl]="state.latestImageUrl"
      (demoActiveChange)="state.demoMode.set($event)"
    ></app-analysis-view>

    <app-analysis-history [analyses]="recentAnalyses()"></app-analysis-history>

    <p class="dashboard__updated">
      Dernière analyse : {{ state.status()?.analyzedAt | date: 'HH:mm:ss' }}
    </p>

    <div class="dashboard__more">
      <a routerLink="/historique" class="btn btn--secondary">
        <span class="material-symbols-outlined" aria-hidden="true">history</span>
        Voir l'historique complet
      </a>
      <a routerLink="/statistiques" class="btn btn--secondary">
        <span class="material-symbols-outlined" aria-hidden="true">monitoring</span>
        Voir les statistiques
      </a>
    </div>
  `,
  styles: `
    :host { display: block; display: flex; flex-direction: column; gap: 1.25rem; }
    .card {
      background: var(--card-bg, #fff); border: 1px solid var(--border-color);
      border-radius: var(--radius); padding: 1rem 1.25rem; box-shadow: var(--shadow-sm);
    }
    .card h2 { margin: 0 0 0.75rem; font-size: 1rem; font-weight: 600; color: var(--navy); }
    .belt { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; }
    .belt__value { display: block; font-size: 1.45rem; font-weight: 800; color: var(--navy); }
    .belt__label { font-size: 0.72rem; color: var(--text-muted); }
    .dashboard__more { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .dashboard__updated { margin: 0; font-size: 0.75rem; color: var(--text-muted); }
    @media (max-width: 1024px) {
      .belt { grid-template-columns: repeat(2, 1fr); }
    }
  `,
})
export class Dashboard {
  protected readonly state = inject(SystemStateService);

  /** 8 dernieres analyses reels seulement (l'historique complet est sur /historique). */
  recentAnalyses() {
    return this.state.recent().slice(0, 8);
  }
}