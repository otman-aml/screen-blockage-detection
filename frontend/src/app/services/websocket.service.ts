import { Injectable, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';
import { environment } from '../environment';
import { WebSocketStatusMessage } from '../models/websocket-status-message.model';

/**
 * Client WebSocket (STOMP over SockJS) vers le backend (Phase 5).
 *
 * <p>Le frontend affiche EXACTEMENT le statut recu du backend : il ne
 * genere/recalcule jamais d'etat metier.
 *
 * <p>Le flux RxJS {@link messages$} emet a chaque message recu sur
 * {@code /topic/status} : {@code { status, blockagePercentage, timestamp }}.
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private readonly messagesSubject = new Subject<WebSocketStatusMessage>();
  private client: Client | null = null;

  /** Etat de connexion WebSocket (indicateur header, Phase 7). */
  readonly connected = signal(false);

  /** Flux des messages de statut recus en temps reel. */
  readonly messages$: Observable<WebSocketStatusMessage> =
    this.messagesSubject.asObservable();

  /** Connecte au endpoint SockJS et s'abonne au topic de statut. */
  connect(): void {
    if (this.client) {
      return; // deja connecte
    }

    const socket = new SockJS(`${environment.wsBaseUrl}${environment.wsEndpoint}`);
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected.set(true);
        client.subscribe(environment.statusTopic, (message: IMessage) => {
          this.handleMessage(message);
        });
      },
      onWebSocketClose: () => {
        this.connected.set(false);
        this.messagesSubject.complete();
      },
    });

    this.client = client;
    client.activate();
  }

  /** Deconnecte proprement. */
  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }

  /**
   * Parse un message STOMP recu et le diffuse.
   * Expose pour les tests unitaires (pas de vrai serveur WebSocket).
   */
  handleMessage(message: IMessage): void {
    try {
      const payload = JSON.parse(message.body) as WebSocketStatusMessage;
      this.messagesSubject.next(payload);
    } catch (err) {
      console.error('Invalid WebSocket status message', message.body, err);
    }
  }
}
