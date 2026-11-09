import { TestBed } from '@angular/core/testing';
import { IMessage } from '@stomp/stompjs';
import { WebSocketService } from './websocket.service';

describe('WebSocketService', () => {
  let service: WebSocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WebSocketService);
  });

  it('handleMessage parse un message valide et le diffuse', () => {
    let received: unknown;
    service.messages$.subscribe((m) => {
      received = m;
    });

    service.handleMessage({
      body: JSON.stringify({
        status: 'WARNING',
        blockagePercentage: 45,
        timestamp: '2026-08-31T10:10:00Z',
      }),
    } as IMessage);

    expect(received).toEqual({
      status: 'WARNING',
      blockagePercentage: 45,
      timestamp: '2026-08-31T10:10:00Z',
    });
  });

  it('ignore un JSON invalide sans emettre i evenement', () => {
    const consoleError = vi.spyOn(console, 'error');
    let emitted = false;
    service.messages$.subscribe(() => {
      emitted = true;
    });

    service.handleMessage({ body: '{pas du json' } as IMessage);

    expect(emitted).toBe(false);
    expect(consoleError).toHaveBeenCalled();
  });

  it('disconnect sans connexion active ne plante pas et nettoie', () => {
    const client = (service as unknown as { client: null }).client;
    expect(client).toBeNull();
    expect(() => service.disconnect()).not.toThrow();
  });
});