import {
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  viewChild,
  AfterViewInit,
  input,
} from '@angular/core';
import { LatestAnalysis } from '../../models/detection.model';

/**
 * Overlay des roches detectees sur l'image analysee (Phase 6).
 *
 * <p>Un {@code <img>} affiche la derniere analyse (GET /api/detections/latest/image)
 * et un {@code <canvas>} est superpose dessus pour dessiner les polygones de
 * masques {@code mask} fournis par le BACKEND (GET /api/detections/latest).
 *
 * <p>Les coordonnees des polygones sont dans l'espace de l'image originale
 * ({@link LatestAnalysis#imageWidth imageWidth}/{@link LatestAnalysis#imageHeight imageHeight}).
 * Ce composant les met a l'echelle de la taille a l'ecran avant de les tracer :
 * c'est un simple rendu graphique, aucune decision metier.
 */
@Component({
  selector: 'app-detection-overlay',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="overlay">
      @if (!analysis()) {
        <p class="overlay__empty">Aucune analyse affichable.</p>
      } @else {
        <img
          #img
          class="overlay__img"
          [src]="imageUrl()"
          alt="Derniere frame analysee"
          (load)="redraw()"
        />
        <canvas
          #canvas
          class="overlay__canvas"
          aria-hidden="true"
        ></canvas>
        <div class="overlay__info">
          Vision: {{ numberOfRock }} roche(s) ·
          Blocage: {{ blockageText }}
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
    .overlay {
      position: relative;
      display: inline-block;
      max-width: 100%;
      overflow: hidden;
      border-radius: 8px;
      background: #000;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    }
    .overlay__img {
      display: block;
      max-width: 100%;
      height: auto;
    }
    .overlay__canvas {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      pointer-events: none;
    }
    .overlay__empty {
      color: #888;
      font-style: italic;
      padding: 1rem;
      background: #fff;
    }
    .overlay__info {
      position: absolute;
      left: 0.5rem;
      bottom: 0.5rem;
      background: rgba(0, 0, 0, 0.7);
      color: #fff;
      padding: 0.25rem 0.6rem;
      border-radius: 4px;
      font-size: 0.8rem;
    }
  `,
})
export class DetectionOverlay implements AfterViewInit {
  /** Derniere analyse (inclut les polygones de masques). */
  readonly analysis = input<LatestAnalysis | null>(null);

  /** URL image de la derniere analyse (GET .../latest/image). */
  readonly imageUrl = input<string>('');

  /**
   * Affiche les masques (vue analysee) ou l'image seule (vue originale,
   * Phase 7 : comparaison cote a cote).
   */
  readonly showMasks = input(true);

  private readonly canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly imgRef = viewChild<ElementRef<HTMLImageElement>>('img');

  constructor() {
    // Re-trace des qu'une nouvelle analyse ou nouvelle URL arrive et que
    // l'image est (deja) chargee. Le microtask laisse le temps a la branche
    // @if de rendre le <canvas>/<img> et aux viewChild de se resoudre.
    effect(() => {
      void this.analysis();
      void this.imageUrl();
      queueMicrotask(() => this.redraw());
    });
  }

  ngAfterViewInit(): void {
    this.redraw();
  }

  /** Compte d'affichage (info bas de frame). */
  get numberOfRock(): number {
    return this.analysis()?.numberOfDetections ?? 0;
  }

  /** Texte de la part de blocage fenêtre. */
  get blockageText(): string {
    const p = this.analysis()?.blockagePercentage;
    return p === undefined || p === null ? '—' : `${p.toFixed(2).replace('.', ',')} %`;
  }

  /**
   * Dessine l'overlay : met les polygones a l'echelle de la taille a l'ecran
   * puis les trace. Appele sur chargement de l'image / changement d'analyse.
   */
  redraw(): void {
    const analysis = this.analysis();
    const canvas = this.canvasRef()?.nativeElement;
    const img = this.imgRef()?.nativeElement;

    if (!analysis || !canvas || !img) {
      return;
    }
    if (img.naturalWidth === 0 || img.naturalHeight === 0) {
      return; // image pas encore chargee ; le handler (load) relancera redraw.
    }

    // Taille a l'ecran de l'image.
    const cssWidth = img.clientWidth;
    const cssHeight = img.clientHeight;
    if (cssWidth === 0 || cssHeight === 0) {
      return;
    }

    // L'entete du canvas matche la taille a l'ecran pour un rendu net.
    canvas.width = cssWidth;
    canvas.height = cssHeight;

    const ctx = canvas.getContext?.('2d') as CanvasRenderingContext2D | null;
    if (!ctx) {
      return;
    }

    const scaleX = cssWidth / analysis.imageWidth;
    const scaleY = cssHeight / analysis.imageHeight;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (!this.showMasks()) {
      return; // Vue "image originale" : aucun masque trace.
    }

    for (const detection of analysis.detections) {
      ctx.beginPath();
      const mask = detection.mask;
      if (mask.length === 0) {
        continue;
      }
      const [firstX, firstY] = mask[0];
      ctx.moveTo(firstX * scaleX, firstY * scaleY);
      for (let i = 1; i < mask.length; i++) {
        const [x, y] = mask[i];
        ctx.lineTo(x * scaleX, y * scaleY);
      }
      ctx.closePath();
      ctx.fillStyle = 'rgba(211, 47, 47, 0.25)';
      ctx.fill();
      ctx.strokeStyle = '#d32f2f';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Etiquette sur le premier sommet.
      ctx.fillStyle = '#d32f2f';
      ctx.font = 'bold 12px sans-serif';
      ctx.fillText(
        `${detection.className} ${(detection.confidence * 100).toFixed(0)}%`,
        firstX * scaleX,
        Math.max(firstY * scaleY - 4, 12),
      );
    }
  }
}