import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { WebSocketService } from './websocket.service';
import { ToastService } from './toast.service';
import { Status } from '../models/status.model';
import { StatusEvent } from '../models/event.model';
import { LatestAnalysis, RecentAnalysis } from '../models/detection.model';
import { WebSocketStatusMessage } from '../models/websocket-status-message.model';

/**
 * Etat partage de l'application (multi-pages, routing Angular).
 *
 * <p>Source unique de verite pour : etat metier du backend, evenements,
 * analyses recentes et derniere analyse (masks). Les pages se contentent
 * de lire ces signaux ; la decision metier reste 100 % backend.
 */
@Injectable({ providedIn: 'root' })
export class SystemStateService {
  private readonly api = inject(ApiService);
  private readonly ws = inject(WebSocketService);
  private readonly toasts = inject(ToastService);

  readonly status = signal<Status | null>(null);
  readonly events = signal<StatusEvent[]>([]);
  readonly recent = signal<RecentAnalysis[]>([]);
  readonly latestAnalysis = signal<LatestAnalysis | null>(null);
  /** Mode demo accelere (analyse 1 img/s) — jamais la cadence production. */
  readonly demoMode = signal(false);

  readonly wsConnected = this.ws.connected;
  readonly latestImageUrl = this.api.latestImageUrl();
  readonly isCritical = () => this.status()?.status === 'CRITICAL';

  /** Charge les donnees initiales via REST (appelle au demarrage de l'app). */
  refresh(): void {
    this.api.getStatus().subscribe({
      next: (status) => this.status.set(status),
      error: (err) => console.warn('[state] getStatus a echoue', err),
    });
    this.api.getEvents().subscribe({
      next: (events) => this.events.set(events),
      error: (err) => console.warn('[state] getEvents a echoue', err),
    });
    this.api.getLatestAnalysis().subscribe({
      next: (analysis) => this.latestAnalysis.set(analysis),
      error: (err) => console.warn('[state] getLatestAnalysis a echoue', err),
    });
    this.refreshAnalyses();
  }

  /**
   * Recharge uniquement l'historique des analyses (apres une nouvelle
   * analyse) : une analyse = une ligne, meme sans changement de statut.
   */
  refreshAnalyses(): void {
    this.api.getRecentAnalyses().subscribe({
      next: (analyses) => this.recent.set(analyses),
      error: (err) => console.warn('[state] getRecentAnalyses a echoue', err),
    });
  }

  /** Connecte le WebSocket et diffuse les messages temps reel. */
  connectWebSocket(): void {
    this.ws.messages$.subscribe((message) => this.applyWebSocketMessage(message));
    this.ws.connect();
  }

  disconnectWebSocket(): void {
    this.ws.disconnect();
  }

  private applyWebSocketMessage(message: WebSocketStatusMessage): void {
    const previous = this.status()?.status;
    const current = this.status();

    this.status.set({
      status: message.status,
      blockagePercentage: message.blockagePercentage,
      framesAboveThresholdPercentage: current?.framesAboveThresholdPercentage ?? 0,
      oversizedRocks: current?.oversizedRocks ?? 0,
      sampleCount: current?.sampleCount ?? 0,
      windowStart: current?.windowStart ?? null,
      analyzedAt: message.timestamp || current?.analyzedAt || null,
    });

    if (previous && previous !== message.status) {
      this.notifyTransition(previous, message.status, message.blockagePercentage);
      this.api.getEvents().subscribe({
        next: (events) => this.events.set(events),
        error: (err) => console.warn('[state] getEvents a echoue', err),
      });
    }
  }

  private notifyTransition(from: Status['status'], to: Status['status'], blockage: number): void {
    if (to === 'CRITICAL') {
      this.toasts.show('critical', 'ALERTE CRITIQUE',
        `Blocage important détecté (${blockage.toFixed(1)} %) — intervention requise.`, 10000);
    } else if (to === 'WARNING') {
      this.toasts.show('warning', 'Passage en WARNING',
        `Attention : niveau de blocage élevé (${blockage.toFixed(1)} %).`);
    } else {
      this.toasts.show('success', 'Retour à la normale',
        `Le système est revenu en NORMAL (${blockage.toFixed(1)} % de blocage).`);
    }
  }
}