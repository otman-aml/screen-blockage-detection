import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecentAnalysis } from '../../models/detection.model';

/**
 * HISTORIQUE DES ANALYSES : une ligne par analyse d'image (source reelle
 * GET /api/detections/recent). Une analyse sans changement de statut
 * systeme apparait quand meme : UNE ANALYSE = UNE LIGNE.
 *
 * <p>La colonne ETAT affiche la classification de l'image calculee par le
 * backend (seuils configures). Le frontend ne recalcule jamais un statut.
 * Export CSV reel : memes donnees que le tableau.
 */
@Component({
  selector: 'app-analysis-history',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <section class="analysis-history" id="section-history">
      <header class="analysis-history__head">
        <h2>Historique des analyses</h2>
        <button type="button" class="analysis-history__csv" (click)="exportCsv()"
                [disabled]="analyses().length === 0"
                aria-label="Exporter l'historique des analyses en CSV">
          <span class="material-symbols-outlined" aria-hidden="true">download</span>
          Export CSV
        </button>
      </header>

      @if (analyses().length === 0) {
        <p class="analysis-history__empty">Aucune analyse pour l'instant.</p>
      } @else {
        <div class="analysis-history__scroll" [class.analysis-history__scroll--active]="scrollable()">
          <table class="analysis-history__table">
            <thead>
              <tr>
                <th>Heure</th>
                <th>État</th>
                <th>Description</th>
                <th>Blocage image</th>
                <th>Roches</th>
              </tr>
            </thead>
            <tbody>
              @for (analysis of analyses(); track analysis.id) {
                <tr>
                  <td>{{ analysis.analyzedAt | date: 'HH:mm:ss' }}</td>
                  <td>
                    <span
                      class="analysis-history__badge"
                      [class.status-normal]="analysis.status === 'NORMAL'"
                      [class.status-warning]="analysis.status === 'WARNING'"
                      [class.status-critical]="analysis.status === 'CRITICAL'"
                    >
                      {{ analysis.status }}
                    </span>
                  </td>
                  <td class="analysis-history__desc">{{ describe(analysis) }}</td>
                  <td>{{ analysis.blockagePercentage | number: '1.0-2' }} %</td>
                  <td>{{ analysis.numberOfDetections }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: `
    :host { display: block; }
    .analysis-history {
      background: var(--card-bg, #ffffff);
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      padding: 1rem 1.25rem;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    }
    .analysis-history__head {
      display: flex; align-items: center; justify-content: space-between;
      gap: 0.75rem; margin-bottom: 0.75rem;
    }
    h2 { margin: 0; font-size: 1rem; font-weight: 600; }
    .analysis-history__csv {
      display: inline-flex; align-items: center; gap: 0.4rem;
      border: 1px solid var(--border-color); background: #fff; color: var(--navy);
      border-radius: 8px; padding: 0.45rem 0.85rem; font-size: 0.8rem;
      font-weight: 600; cursor: pointer; font-family: inherit;
    }
    .analysis-history__csv .material-symbols-outlined { font-size: 18px; }
    .analysis-history__csv:hover:not(:disabled) { background: #f8fafc; border-color: #cbd5e1; }
    .analysis-history__csv:disabled { opacity: 0.5; cursor: not-allowed; }
    .analysis-history__scroll--active {
      max-height: 520px; overflow-y: auto;
      border: 1px solid rgba(0, 0, 0, 0.05); border-radius: 6px;
    }
    .analysis-history__empty { color: #888; font-style: italic; }
    .analysis-history__table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
    .analysis-history__table th,
    .analysis-history__table td {
      text-align: left; padding: 0.4rem 0.5rem;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }
    .analysis-history__table th {
      color: #666; font-weight: 600; font-size: 0.8rem; text-transform: uppercase;
    }
    .analysis-history__badge {
      display: inline-block; padding: 0.15rem 0.5rem; border-radius: 999px;
      font-weight: 700; font-size: 0.75rem;
    }
    .status-normal { color: #2e7d32; background: #e8f5e9; }
    .status-warning { color: #8a6110; background: #fff3d6; }
    .status-critical { color: #b71c1c; background: #ffebee; }
  `,
})
export class AnalysisHistory {
  /** Analyses reelles du backend (recent d'abord). */
  readonly analyses = input<RecentAnalysis[]>([]);
  /** Scroll interne (page /historique) : table bornee en hauteur. */
  readonly scrollable = input(false);

  /** Description basee sur la classification backend de CETTE image. */
  describe(analysis: RecentAnalysis): string {
    if (analysis.status === 'CRITICAL') {
      return 'Blocage critique détecté';
    }
    if (analysis.status === 'WARNING') {
      return 'Blocage modéré détecté';
    }
    return 'Blocage normal détecté';
  }

  /** Export CSV reel des analyses affichees (telechargement navigateur). */
  exportCsv(): void {
    const rows = this.analyses();
    if (rows.length === 0) return;
    const header = 'heure,etat,description,blocageImage,roches';
    const lines = rows.map((a) => [
      new Date(a.analyzedAt).toISOString(),
      a.status,
      this.describe(a),
      a.blockagePercentage,
      a.numberOfDetections,
    ].join(','));
    const blob = new Blob([[header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'analyses.csv';
    anchor.click();
    URL.revokeObjectURL(url);
  }
}