import { TestBed } from '@angular/core/testing';
import { StatusCard } from './status-card';
import { Status } from '../../models/status.model';

describe('StatusCard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [StatusCard] }).compileComponents();
  });

  function render(status: Status | null): HTMLElement {
    const fixture = TestBed.createComponent(StatusCard);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('affiche l etat fourni par le backend sans le recalculer', () => {
    const status: Status = {
      status: 'WARNING',
      blockagePercentage: 45.5,
      framesAboveThresholdPercentage: 12,
      oversizedRocks: 3,
      sampleCount: 20,
      windowStart: '2026-08-31T09:00:00Z',
      analyzedAt: '2026-08-31T09:04:00Z',
    };

    const el = render(status);
    expect(el.querySelector('.status-card__badge')?.textContent?.trim()).toBe('WARNING');
    expect(el.textContent ?? '').toContain('45');
    expect(el.textContent ?? '').toContain('20');
  });

  it('colore la carte selon l etat (classes) sans decision locale', () => {
    const status: Status = {
      status: 'CRITICAL',
      blockagePercentage: 80,
      framesAboveThresholdPercentage: 90,
      oversizedRocks: 5,
      sampleCount: 20,
      windowStart: null,
      analyzedAt: '2026-08-31T09:04:00Z',
    };

    const el = render(status);
    const card = el.querySelector('.status-card');
    expect(card?.classList.contains('status-critical')).toBe(true);
  });

  it('affiche un message en attente quand aucune donnee', () => {
    const el = render(null);
    expect(el.querySelector('.status-card__waiting')).toBeTruthy();
    expect(el.querySelector('.status-card')?.classList.contains('status-empty')).toBe(true);
  });
});