import { Routes } from '@angular/router';
import { Dashboard } from './components/dashboard/dashboard';
import { AnalysePage } from './pages/analyse-page/analyse-page';
import { StatistiquesPage } from './pages/statistiques-page/statistiques-page';
import { HistoriquePage } from './pages/historique-page/historique-page';

/** Routes reelles de l'application (navigation sidebar = Angular Router). */
export const appRoutes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: Dashboard, title: 'Dashboard — OCP Screen Blockage' },
  { path: 'analyse', component: AnalysePage, title: 'Analyse — OCP Screen Blockage' },
  { path: 'statistiques', component: StatistiquesPage, title: 'Statistiques — OCP Screen Blockage' },
  { path: 'historique', component: HistoriquePage, title: 'Historique — OCP Screen Blockage' },
  { path: '**', redirectTo: 'dashboard' },
];