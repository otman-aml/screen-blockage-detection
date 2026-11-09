import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AnalysisHistory } from '../../components/analysis-history/analysis-history';
import { SystemStateService } from '../../services/system-state.service';

/**
 * Page /historique — historique COMPLET des analyses (une analyse = une ligne),
 * avec scroll interne (la page ne s'allonge pas indefiniment).
 */
@Component({
  selector: 'app-historique-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AnalysisHistory],
  template: `
    <h1 class="page-title">Historique</h1>
    <app-analysis-history [analyses]="state.recent()" [scrollable]="true"></app-analysis-history>
  `,
  styles: `
    :host { display: block; }
    .page-title { margin: 0 0 1rem; font-size: 1.3rem; font-weight: 800; color: var(--navy); }
  `,
})
export class HistoriquePage {
  protected readonly state = inject(SystemStateService);
}