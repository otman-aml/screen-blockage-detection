import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecentAnalysis } from '../../models/detection.model';

/**
 * Zone statistiques (Phase 7) : evolution REELLE du blockagePercentage
 * fourni par GET /api/detections/recent (sparkline SVG, sans dependance).
 */
@Component({
  selector: 'app-stats-zone',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
        <section class="stats" aria-label="Statistiques de blocage">
      <h2>Évolution du blocage (analyses récentes)</h2>
      @if (points().length === 0) {
        <p class="stats__empty">Aucune donnée disponible pour l'instant.</p>
      } @else {
        <svg [attr.viewBox]="'0 0 ' + chartWidth + ' ' + chartHeight" role="img"
             [attr.aria-label]="'Graphique de blocage, ' + points().length + ' points'">
          <!-- Fond -->
          <rect [attr.x]="marginLeft" [attr.y]="marginTop"
                [attr.width]="chartWidth - marginLeft - marginRight"
                [attr.height]="chartHeight - marginTop - marginBottom"
                fill="#f8fafc" rx="4" />
          <!-- Axes -->
          <line [attr.x1]="marginLeft" [attr.y1]="marginTop"
                [attr.x2]="marginLeft" [attr.y2]="chartHeight - marginBottom"
                class="axis axis--y" />
          <line [attr.x1]="marginLeft" [attr.y1]="chartHeight - marginBottom"
                [attr.x2]="chartWidth - marginRight" [attr.y2]="chartHeight - marginBottom"
                class="axis axis--x" />
          <!-- Lignes de seuil -->
          <line [attr.x1]="marginLeft" [attr.y1]="warnY"
                [attr.x2]="chartWidth - marginRight" [attr.y2]="warnY"
                class="threshold threshold--warn" />
          <line [attr.x1]="marginLeft" [attr.y1]="critY"
                [attr.x2]="chartWidth - marginRight" [attr.y2]="critY"
                class="threshold threshold--crit" />
          <!-- Etiquettes seuils -->
          <text [attr.x]="chartWidth - marginRight + 4" [attr.y]="warnY + 4"
                class="threshold-label">25%</text>
          <text [attr.x]="chartWidth - marginRight + 4" [attr.y]="critY + 4"
                class="threshold-label">50%</text>
          <!-- Courbe -->
          <polyline [attr.points]="polylinePoints()" class="curve" fill="none" />
          <!-- Axe Y (étiquettes pourcentages) -->
          <text [attr.x]="marginLeft - 6" [attr.y]="marginTop + 4"
                class="axis-label">100%</text>
          <text [attr.x]="marginLeft - 6" [attr.y]="chartHeight - marginBottom + 12"
                class="axis-label">0%</text>
          <!-- Axe X (étiquette temps) -->
          <text [attr.x]="(chartWidth) / 2" [attr.y]="chartHeight - 4"
                class="axis-label axis-label--x">Heure</text>
          <!-- Axe Y (étiquette blocage) -->
          <text [attr.x]="marginLeft - 38" [attr.y]="chartHeight / 2"
                class="axis-label axis-label--y"
                [attr.transform]="'rotate(-90, ' + (marginLeft - 38) + ', ' + (chartHeight / 2) + ')'">% Blocage</text>
        </svg>
        <p class="stats__meta">
          {{ points().length }} analyses affichées — seuils : WARNING (25 %) / CRITICAL (50 %)
        </p>
      }
  `,
  styles: `
    :host { display: block; }
    .stats {
      background: var(--card-bg, #fff); border: 1px solid rgba(0,0,0,0.08);
      border-radius: 8px; padding: 1rem 1.25rem; box-shadow: 0 1px 3px rgba(0,0,0,0.07);
    }
    h2 { margin: 0 0 0.75rem; font-size: 1rem; font-weight: 600; }
        svg { width: 100%; height: 160px; background: #f8fafc; border-radius: 6px; }
    .curve { stroke: #1f6fb2; stroke-width: 2; fill: none; }
    .threshold--warn { stroke: #f9a825; stroke-dasharray: 4 4; }
    .threshold--crit { stroke: #d32f2f; stroke-dasharray: 4 4; }
    .axis { stroke: #94a3b8; stroke-width: 1; }
    .axis-label { font-size: 10px; fill: #64748b; font-family: inherit; }
    .axis-label--x { text-anchor: middle; }
    .axis-label--y { text-anchor: middle; }
    .threshold-label { font-size: 10px; fill: #64748b; font-family: inherit; }
    .stats__empty { color: #888; font-style: italic; }
    .stats__meta { font-size: 0.72rem; color: #64748b; margin: 0.5rem 0 0; }
  `,
})
export class StatsZone {
  /** Analyses recentes reelles du backend (du plus recent au plus ancien). */
  readonly analyses = input<RecentAnalysis[]>([]);

    chartWidth = 560;
  chartHeight = 160;
  marginLeft = 50;
  marginRight = 40;
  marginTop = 20;
  marginBottom = 40;

  warnY = this.chartHeight - this.marginTop - (25 / 100) * (this.chartHeight - this.marginTop - this.marginBottom);
  critY = this.chartHeight - this.marginTop - (50 / 100) * (this.chartHeight - this.marginTop - this.marginBottom);

  /** Points du graphe : chronologiques (ancien -> recent). */
  readonly points = computed(() => [...this.analyses()].reverse());

    polylinePoints(): string {
    const pts = this.points();
    if (pts.length === 0) {
      return '';
    }
    const plotWidth = this.chartWidth - this.marginLeft - this.marginRight;
    const plotHeight = this.chartHeight - this.marginTop - this.marginBottom;
    const step = pts.length > 1 ? plotWidth / (pts.length - 1) : 0;
    return pts
      .map((a, i) => {
        const x = this.marginLeft + i * step;
        const y = this.chartHeight - this.marginBottom - (Math.min(Math.max(a.blockagePercentage, 0), 100) / 100) * plotHeight;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  }
}
