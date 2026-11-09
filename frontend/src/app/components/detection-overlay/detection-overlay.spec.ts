import { TestBed } from '@angular/core/testing';
import { DetectionOverlay } from './detection-overlay';
import { LatestAnalysis } from '../../models/detection.model';

describe('DetectionOverlay', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [DetectionOverlay] }).compileComponents();
  });

  function render(analysis: LatestAnalysis | null, imageUrl: string): { el: HTMLElement; fixture: any } {
    const fixture = TestBed.createComponent(DetectionOverlay);
    fixture.componentRef.setInput('analysis', analysis);
    fixture.componentRef.setInput('imageUrl', imageUrl);
    fixture.detectChanges();
    return { el: fixture.nativeElement as HTMLElement, fixture };
  }

  const analysis: LatestAnalysis = {
    id: 7,
    imageWidth: 640,
    imageHeight: 480,
    blockagePercentage: 42,
    analyzedAt: '2026-08-31T10:00:00Z',
    numberOfDetections: 1,
    detections: [
      {
        id: 1,
        classId: 0,
        className: 'rock',
        confidence: 0.9,
        widthCm: 30,
        heightCm: 20,
        oversized: false,
        mask: [
          [1, 1],
          [2, 1],
          [2, 2],
        ],
      },
    ],
  };

  it('affiche le message vide quand aucune analyse', () => {
    const { el } = render(null, '');
    expect(el.querySelector('.overlay__empty')).toBeTruthy();
  });

  it('affiche l image a l URL fournie et l entete d info', () => {
    const { el } = render(analysis, 'http://localhost:8080/api/detections/latest/image');
    const img = el.querySelector('img.overlay__img') as HTMLImageElement;
    expect(img?.getAttribute('src')).toBe('http://localhost:8080/api/detections/latest/image');
    expect(el.querySelector('.overlay__canvas')).toBeTruthy();
    expect(el.querySelector('.overlay__info')?.textContent ?? '').toContain('42');
  });

  it("ne plante pas quand le contexte canvas n'est pas disponible (jsdom)", () => {
    const { fixture } = render(analysis, 'image.png');
    // jsdom ne fournit pas getContext('2d') : redraw() doit etre tolerant.

    expect(() => fixture.detectChanges()).not.toThrow();
  });
});