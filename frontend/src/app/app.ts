import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SystemStateService } from './services/system-state.service';
import { AppHeader } from './components/app-header/app-header';
import { AppSidebar } from './components/app-sidebar/app-sidebar';
import { AppFooter } from './components/app-footer/app-footer';
import { ToastZone } from './components/toast-zone/toast-zone';

/**
 * Shell de l'application : header + sidebar fixe + pages routees.
 * Le WebSocket et le chargement REST sont geres ici (une seule fois),
 * l'etat est partage entre toutes les pages via SystemStateService.
 */
@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, AppHeader, AppSidebar, AppFooter, ToastZone],
    template: `
    <app-header
      [status]="state.status()"
      [imageStatus]="latestImageStatus()"
      [wsConnected]="state.wsConnected()"
      [demoMode]="state.demoMode()"
    ></app-header>

    <div class="shell">
      <app-sidebar></app-sidebar>
      <main class="shell__content">
        <router-outlet></router-outlet>
        <app-footer></app-footer>
        <app-toast-zone></app-toast-zone>
      </main>
    </div>
  `,
  styles: `
    :host { display: block; background: #eef2f6; min-height: 100vh; }
    app-header { position: sticky; top: 0; z-index: 100; display: block; }
    .shell { display: flex; min-height: calc(100vh - 56px); }
    app-sidebar { width: 230px; flex-shrink: 0; }
    .shell__content {
      flex: 1; padding: 1.5rem; min-width: 0; overflow-x: hidden;
    }
    @media (max-width: 900px) {
      .shell { flex-direction: column; }
      app-sidebar { width: 100%; }
    }
  `,
})
export class App implements OnInit, OnDestroy {
  protected readonly state = inject(SystemStateService);

  /**
   * Statut de la DERNIERE analyse image (NORMAL/WARNING/CRITICAL) déduit
   * du dernier recentAnalysis. C'est ce statut qui apparait dans le header,
   * en coherence immediable avec le blocage affiche.
   */
  latestImageStatus(): string | null {
    const last = this.state.recent()[0];
    return last?.status ?? null;
  }

  ngOnInit(): void {
    this.state.refresh();
    this.state.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.state.disconnectWebSocket();
  }
}
