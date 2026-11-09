import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Footer application : mention OCP Group conforme a la maquette,
 * logo officiel deja integre (frontend/public/ocp-logo.jpg).
 */
@Component({
  selector: 'app-footer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer class="footer">
      <img src="ocp-logo.jpg" alt="OCP Group" class="footer__logo" />
      <span>© 2026 OCP Group — OCP Screen Blockage Detection</span>
      <span class="footer__sep">·</span>
      <span>Supervision industrielle en temps réel</span>
    </footer>
  `,
  styles: `
    :host { display: block; }
    .footer {
      display: flex; align-items: center; justify-content: center; gap: 0.5rem;
      padding: 1rem; border-top: 1px solid var(--border-color);
      color: var(--text-muted); font-size: 0.75rem; background: var(--surface);
    }
    .footer__logo { width: 20px; height: 20px; object-fit: contain; }
    .footer__sep { opacity: 0.5; }
  `,
})
export class AppFooter {}