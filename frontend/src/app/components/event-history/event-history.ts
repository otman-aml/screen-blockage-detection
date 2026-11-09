import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatusEvent } from '../../models/event.model';

/**
 * Historique des transitions d'etat recues du backend (GET /api/events).
 *
 * <p>La liste est affichee du plus recent au plus ancien. Le libelle des
 * etats vient tel quel du backend (regle d'architecture: pas de
 * re-calcul metier cote UI). Export CSV reel implemente (Phase 7).
 */
@Component({
  selector: 'app-event-history',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <section class="event-history" id="section-history">
      <header class="event-history__head">
        <h2>Historique des transitions</h2>
        <button type="button" class="event-history__csv" (click)="exportCsv()"
                [disabled]="events().length === 0"
                aria-label="Exporter l'historique en CSV">
          <span class="material-symbols-outlined" aria-hidden="true">download</span>
          Export CSV
        </button>
      </header>

      @if (events().length === 0) {
        <p class="event-history__empty">Aucun evenement pour l'instant.</p>
      } @else {
        <div class="event-history__scroll" [class.event-history__scroll--active]="scrollable()">
          <table class="event-history__table">
          <thead>
            <tr>
              <th>Heure</th>
              <th>Transition</th>
              <th>Description</th>
              <th>Blocage image</th>
              <th>Frames &gt; seuil</th>
              <th>Fenêtre</th>
              <th>Roches</th>
            </tr>
          </thead>
          <tbody>
            @for (event of events(); track event.id) {
              <tr>
                <td>{{ event.timestamp | date: 'HH:mm:ss' }}</td>
                <td>
                  <span
                    class="event-history__badge"
                    [class.status-normal]="event.to === 'NORMAL'"
                    [class.status-warning]="event.to === 'WARNING'"
                    [class.status-critical]="event.to === 'CRITICAL'"
                  >
                    {{ event.from }} &rarr; {{ event.to }}
                  </span>
                </td>
                <td class="event-history__desc">{{ describe(event) }}</td>
                <td>{{ event.blockagePercentage | number: '1.0-2' }} %</td>
                <td>{{ decisionMetricLabel(event) }}</td>
                <td>{{ event.framesInWindow }}</td>
                <td>{{ event.oversizedRocks }}</td>
              </tr>
            }
          </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: `
    :host {
      display: block;
    }
    .event-history {
      background: var(--card-bg, #ffffff);
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      padding: 1rem 1.25rem;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    }
    .event-history__head {
      display: flex; align-items: center; justify-content: space-between;
      gap: 0.75rem; margin-bottom: 0.75rem;
    }
    h2 {
      margin: 0 0 0.75rem;
      font-size: 1rem;
      font-weight: 600;
    }
    .event-history__head h2 { margin: 0; }
    .event-history__csv {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      border: 1px solid var(--border-color);
      background: #fff;
      color: var(--navy);
      border-radius: 8px;
      padding: 0.45rem 0.85rem;
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      font-family: inherit;
    }
    .event-history__csv .material-symbols-outlined { font-size: 18px; }
    .event-history__csv:hover:not(:disabled) { background: #f8fafc; border-color: #cbd5e1; }
    .event-history__csv:disabled { opacity: 0.5; cursor: not-allowed; }
    .event-history__desc { font-size: 0.8rem; color: #546e7a; }
    .event-history__scroll--active {
      max-height: 520px;
      overflow-y: auto;
      border: 1px solid rgba(0, 0, 0, 0.05);
      border-radius: 6px;
    }
    .event-history__empty {
      color: #888;
      font-style: italic;
    }
    .event-history__table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.875rem;
    }
    .event-history__table th,
    .event-history__table td {
      text-align: left;
      padding: 0.4rem 0.5rem;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }
    .event-history__table th {
      color: #666;
      font-weight: 600;
      font-size: 0.8rem;
      text-transform: uppercase;
    }
    .event-history__badge {
      display: inline-block;
      padding: 0.15rem 0.5rem;
      border-radius: 999px;
      font-weight: 700;
      font-size: 0.75rem;
    }
    .status-normal {
      color: #2e7d32;
      background: #e8f5e9;
    }
    .status-warning {
      color: #8a6110;
      background: #fff3d6;
    }
    .status-critical {
      color: #b71c1c;
      background: #ffebee;
    }
  `,
})
export class EventHistory {
  /** Evenements a afficher (ordre backend, recent = premier). */
  readonly events = input<StatusEvent[]>([]);
  /** Active le scroll interne (page /historique) : table bornee en hauteur. */
  readonly scrollable = input(false);

  /**
   * Affiche la metrique decisionnelle reelle.
   * Une transition vers WARNING/CRITICAL exige >= 50 % de frames au-dessus du
   * seuil : une valeur 0 est donc IMPOSSIBLE et signale une ancienne donnee
   * backfillee avant l ajout de la colonne -> on affiche em-dash plutot
   * qu une fausse valeur 0.
   */
  decisionMetricLabel(event: StatusEvent): string {
    if ((event.to === 'WARNING' || event.to === 'CRITICAL')
        && event.framesAboveThresholdPercentage === 0) {
      return '\u2014';
    }
    return event.framesAboveThresholdPercentage.toFixed(1) + ' %';
  }

  /** Description lisible basee sur la cause decisionnelle reelle (backend). */
  describe(event: StatusEvent): string {
    if (event.to === 'CRITICAL') {
      return 'Blocage critique soutenu sur la fenetre (detecte par le backend)';
    }
    if (event.to === 'WARNING') {
      return 'Elevation des frames au-dessus du seuil WARNING';
    }
    return 'Indicateur de blocage revenu sous le seuil';
  }

  /** Export CSV reel des evenements affiches. */
  exportCsv(): void {
    const rows = this.events();
    if (rows.length === 0) return;
    const header = 'id,timestamp,from,to,blockagePercentage,oversizedRocks,framesInWindow,framesAboveThresholdPercentage';
    const lines = rows.map((e) => [e.id, e.timestamp, e.from, e.to, e.blockagePercentage, e.oversizedRocks, e.framesInWindow, e.framesAboveThresholdPercentage].join(','));
    const blob = new Blob([[header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'events.csv';
    anchor.click();
    URL.revokeObjectURL(url);
  }
}