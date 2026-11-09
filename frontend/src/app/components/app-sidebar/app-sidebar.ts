import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  label: string;
  route: string;
  icon: string;
}

/**
 * Sidebar de navigation FIXE (visible pendant le scroll) utilisant le
 * Angular Router : chaque entree navigue vers une vraie page.
 * Seules les fonctionnalites reellement implementees sont affichees.
 * Les icones utilisent Material Symbols (pas d'emojis).
 */
@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <div class="sidebar__brand">
        <img src="ocp-logo.jpg" alt="OCP Group" class="sidebar__logo" />
        <div class="sidebar__title">
          <strong>OCP Screen Blockage</strong>
          <span>Supervision en temps réel</span>
        </div>
      </div>

      <nav class="nav" aria-label="Navigation principale">
        @for (item of items; track item.route) {
          <a class="nav__item" [routerLink]="item.route" routerLinkActive="nav__item--active"
             [routerLinkActiveOptions]="{ exact: true }">
            <span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
            {{ item.label }}
          </a>
        }
      </nav>
    </aside>
  `,
  styles: `
    :host { display: block; }
    .sidebar {
      width: 230px;
      background: var(--surface);
      color: var(--text-primary);
      border-right: 1px solid var(--border-color);
      display: flex;
      flex-direction: column;
      flex-shrink: 0;
      position: sticky;
      top: 0;
      height: 100vh;
      overflow-y: auto;
    }
    .sidebar__brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1.1rem 1rem;
      border-bottom: 1px solid var(--border-color);
    }
    .sidebar__logo {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      object-fit: contain;
    }
    .sidebar__title {
      display: flex;
      flex-direction: column;
      line-height: 1.25;
    }
    .sidebar__title strong {
      font-size: 0.82rem;
      font-weight: 700;
      color: var(--navy);
    }
    .sidebar__title span {
      font-size: 0.68rem;
      color: var(--text-muted);
    }
    .nav {
      display: flex;
      flex-direction: column;
      gap: 2px;
      padding: 0.9rem 0.75rem;
    }
    .nav__item {
      display: flex;
      align-items: center;
      gap: 0.7rem;
      padding: 0.65rem 0.85rem;
      border-radius: 8px;
      color: var(--text-secondary);
      text-decoration: none;
      font-size: 0.86rem;
      font-weight: 500;
      transition: background 0.15s ease, color 0.15s ease;
    }
    .nav__item:hover {
      background: #f1f5f9;
      color: var(--navy);
    }
    .nav__item--active,
    .nav__item--active:hover {
      background: #e3eff9;
      color: var(--brand);
      font-weight: 600;
    }
    .nav__item .material-symbols-outlined {
      font-size: 20px;
      color: var(--text-muted);
    }
    .nav__item:hover .material-symbols-outlined {
      color: var(--navy);
    }
    .nav__item--active .material-symbols-outlined {
      color: var(--brand);
    }
    @media (max-width: 900px) {
      .sidebar { width: 100%; height: auto; min-height: auto; position: static; }
      .sidebar__brand { padding: 0.75rem; }
      .nav { flex-direction: row; flex-wrap: wrap; padding: 0.5rem; }
    }
  `,
})
export class AppSidebar {
  readonly items: NavItem[] = [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    { label: 'Analyse', route: '/analyse', icon: 'analytics' },
    { label: 'Statistiques', route: '/statistiques', icon: 'monitoring' },
    { label: 'Historique', route: '/historique', icon: 'history' },
  ];
}
