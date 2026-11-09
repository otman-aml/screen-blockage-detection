import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Status } from '../../models/status.model';
import { StatusEvent } from '../../models/event.model';
import { RecentAnalysis } from '../../models/detection.model';

/**
 * Zone KPI (Phase 7). Toutes les valeurs proviennent des APIs reelles du
 * backend ; aucune metrique fictive. Les valeurs non disponibles sont
 * affichees honnetement ("—").
 */
@Component({
  selector: 'app-kpi-zone',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <section class="kpis" aria-label="Indicateurs clés">
      <div class="kpi">
        <span class="material-symbols-outlined kpi__icon" aria-hidden="true">image</span>
        <div class="kpi__content">
          <span class="kpi__value">{{ totalAnalyses() }}</span>
          <span class="kpi__label">Images analysées</span>
        </div>
      </div>
      <div class="kpi" [class.kpi--warn]="activeAlerts() > 0">
        <span class="material-symbols-outlined kpi__icon" aria-hidden="true">warning</span>
        <div class="kpi__content">
          <span class="kpi__value">{{ activeAlerts() }}</span>
          <span class="kpi__label">Alertes actives</span>
        </div>
      </div>
      <div class="kpi" [class.kpi--crit]="criticalCount() > 0">
        <span class="material-symbols-outlined kpi__icon" aria-hidden="true">error</span>
        <div class="kpi__content">
          <span class="kpi__value">{{ criticalCount() }}</span>
          <span class="kpi__label">Événements critiques</span>
        </div>
      </div>
      <div class="kpi">
        <span class="material-symbols-outlined kpi__icon" aria-hidden="true">schedule</span>
        <div class="kpi__content">
          <span class="kpi__value kpi__value--small">
            {{ lastAnalysisTime() ? (lastAnalysisTime() | date: 'HH:mm:ss') : '—' }}
          </span>
          <span class="kpi__label">Dernière analyse</span>
        </div>
      </div>
    </section>
  `,
  styles: `
    :host { display: block; }
    .kpis {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
    }
    .kpi {
      background: var(--card-bg, #fff);
      border: 1px solid var(--border-color);
      border-radius: var(--radius);
      padding: 1rem 1.1rem;
      box-shadow: var(--shadow-sm);
      display: flex;
      align-items: center;
      gap: 0.9rem;
    }
    .kpi__icon {
      font-size: 28px;
      color: var(--brand);
      flex-shrink: 0;
    }
    .kpi--warn .kpi__icon { color: var(--status-warning); }
    .kpi--crit .kpi__icon { color: var(--status-critical); }
    .kpi__content { display: flex; flex-direction: column; gap: 0.15rem; }
    .kpi__label { font-size: 0.72rem; color: var(--text-muted); font-weight: 500; }
    .kpi__value { font-size: 1.6rem; font-weight: 800; color: var(--navy); line-height: 1.1; }
    .kpi__value--small { font-size: 1.2rem; }
    .kpi--warn { border-left: 4px solid var(--status-warning); }
    .kpi--crit { border-left: 4px solid var(--status-critical); }
  `,
})
export class KpiZone {
  readonly status = input<Status | null>(null);
  readonly events = input<StatusEvent[]>([]);
  /** Analyses recentes reelles (GET /api/detections/recent). */
  readonly analyses = input<RecentAnalysis[]>([]);
  /** Derniere analyse d'image (pour les KPI dynamiques). */
  readonly lastAnalysis = input<RecentAnalysis | null>(null);

  /** Nombre total d'analyses reelles. */
  readonly totalAnalyses = computed(() => this.analyses().length);

  /** Alertes actives : 1 si la derniere analyse est CRITICAL ou WARNING, 0 sinon. */
  readonly activeAlerts = computed(() => {
    const last = this.lastAnalysis();
    return last && last.status !== 'NORMAL' ? 1 : 0;
  });

  /** Evenements critiques : nombre d'analyses CRITICAL dans l'historique. */
  readonly criticalCount = computed(
    () => this.analyses().filter((a) => a.status === 'CRITICAL').length,
  );

  readonly lastAnalysisTime = computed(
    () => this.analyses()[0]?.analyzedAt ?? null,
  );
}
