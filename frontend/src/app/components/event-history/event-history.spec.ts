import { TestBed } from '@angular/core/testing';
import { EventHistory } from './event-history';
import { StatusEvent } from '../../models/event.model';

describe('EventHistory', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [EventHistory] }).compileComponents();
  });

  function render(events: StatusEvent[]): HTMLElement {
    const fixture = TestBed.createComponent(EventHistory);
    fixture.componentRef.setInput('events', events);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('affiche un message vide quand aucune transition', () => {
    const el = render([]);
    expect(el.querySelector('.event-history__empty')).toBeTruthy();
    expect(el.querySelector('table')).toBeNull();
  });

  it('affiche une ligne par transition, avec les etats du backend', () => {
    const events: StatusEvent[] = [
      {
        id: 1,
        from: 'NORMAL',
        to: 'WARNING',
        timestamp: '2026-08-31T10:00:00Z',
        blockagePercentage: 42,
        oversizedRocks: 2,
        framesInWindow: 15,
        framesAboveThresholdPercentage: 50,
      },
      {
        id: 2,
        from: 'WARNING',
        to: 'CRITICAL',
        timestamp: '2026-08-31T10:02:00Z',
        blockagePercentage: 78,
        oversizedRocks: 6,
        framesInWindow: 15,
        framesAboveThresholdPercentage: 100,
      },
    ];

    const el = render(events);
    const rows = el.querySelectorAll('.event-history__table tbody tr');
    expect(rows.length).toBe(2);
    expect(el.textContent ?? '').toContain('NORMAL');
    expect(el.textContent ?? '').toContain('WARNING');
    expect(el.textContent ?? '').toContain('CRITICAL');
  });

  it('affiche les colonnes decisionnelles et la valeur de decision separement', () => {
    const events: StatusEvent[] = [
      {
        id: 3,
        from: 'NORMAL',
        to: 'WARNING',
        timestamp: '2026-08-31T10:00:30Z',
        // Blocage de l'image declenchante tres eleve...
        blockagePercentage: 94.42,
        oversizedRocks: 2,
        framesInWindow: 2,
        // ...mais la DECISION WARNING repose sur le ratio de la fenetre (100 %).
        framesAboveThresholdPercentage: 100,
      },
    ];
    const el = render(events);
    const text = el.textContent ?? '';
    expect(text).toContain('94.42'); // blocage image, affiche tel quel
    expect(text).toContain('100');   // frames > seuil (metrique decisionnelle)
    // La colonne est nommee "Blocage image" -> le 94.42 n'est plus presente comme la
    // valeur ayant declare WARNING (le frontend ne recalcule jamais le statut).
    expect(text).toContain('Blocage image');
    expect(text).toContain('Frames > seuil');
  });
  it('affiche em-dash pour les anciennes donnees backfillees (decision indisponible)', () => {
    // Un WARNING/CRITICAL avec framesAboveThreshold=0 est impossible selon les
    // regles (>=50 % requis) : c'est une ancienne donnee backfillee -> em-dash.
    const events: StatusEvent[] = [
      {
        id: 1, from: 'NORMAL', to: 'WARNING',
        timestamp: '2026-09-05T13:16:57Z',
        blockagePercentage: 94.42, oversizedRocks: 103, framesInWindow: 2,
        framesAboveThresholdPercentage: 0,
      },
    ];
    const el = render(events);
    expect(el.textContent ?? '').toContain('\u2014');
  });


  it('exporte un CSV contenant les memes valeurs que le tableau', async () => {
    const events: StatusEvent[] = [
      {
        id: 4, from: 'NORMAL', to: 'WARNING',
        timestamp: '2026-09-05T14:27:15Z',
        blockagePercentage: 91.94, oversizedRocks: 78, framesInWindow: 2,
        framesAboveThresholdPercentage: 100,
      },
    ];
    const fixture = TestBed.createComponent(EventHistory);
    fixture.componentRef.setInput('events', events);
    fixture.detectChanges();

    let captured: unknown = null;
    const revoke = URL.revokeObjectURL;
    (URL as unknown as { createObjectURL: (b: Blob) => string }).createObjectURL =
      (b: Blob) => { captured = b; return 'blob:x'; };
    (URL as unknown as { revokeObjectURL: (u: string) => void }).revokeObjectURL = () => undefined;

    fixture.componentInstance.exportCsv();
    expect(captured).toBeTruthy();
    expect(captured).toBeInstanceOf(Blob);
    const text = await (captured as Blob).text();
    expect(text).toContain('framesAboveThresholdPercentage');
    expect(text).toContain('4,2026-09-05T14:27:15Z,NORMAL,WARNING,91.94,78,2,100');
    URL.revokeObjectURL = revoke;
  });
});