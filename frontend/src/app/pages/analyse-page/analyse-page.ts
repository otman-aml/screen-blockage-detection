import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AnalysisView } from '../../components/analysis-view/analysis-view';
import { SystemStateService } from '../../services/system-state.service';

/**
 * Page /analyse — analyse detaillee d'une image.
 * Reutilise AnalysisView : images cote a cote + panneau details + actions.
 */
@Component({
  selector: 'app-analyse-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AnalysisView],
  template: `
    <app-analysis-view
      [latestAnalysis]="state.latestAnalysis()"
      [latestImageUrl]="state.latestImageUrl"
      (demoActiveChange)="state.demoMode.set($event)"
    ></app-analysis-view>
  `,
  styles: `
    :host { display: block; }
  `,
})
export class AnalysePage {
  protected readonly state = inject(SystemStateService);
}