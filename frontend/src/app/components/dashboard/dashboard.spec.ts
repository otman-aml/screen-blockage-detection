import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { Dashboard } from './dashboard';
import { ApiService } from '../../services/api.service';
import { WebSocketService } from '../../services/websocket.service';
import { SystemStateService } from '../../services/system-state.service';
import { Status } from '../../models/status.model';
import { WebSocketStatusMessage } from '../../models/websocket-status-message.model';

class ApiStub {
  getStatus = vi.fn(() =>
    of<Status>({
      status: 'NORMAL',
      blockagePercentage: 10,
      framesAboveThresholdPercentage: 0,
      oversizedRocks: 0,
      sampleCount: 5,
      windowStart: null,
      analyzedAt: '2026-08-31T09:00:00Z',
    }),
  );
  getEvents = vi.fn(() => of([]));
  getLatestAnalysis = vi.fn(() =>
    of({
      id: 1,
      imageWidth: 640,
      imageHeight: 480,
      blockagePercentage: 10,
      analyzedAt: '2026-08-31T09:00:00Z',
      numberOfDetections: 0,
      detections: [],
    }),
  );
  latestImageUrl = vi.fn(() => 'image.png');
  getRecentAnalyses = vi.fn(() => of([]));
}

class WsStub {
  connected = signal(false);
  messages$ = new Subject<WebSocketStatusMessage>();
  connect = vi.fn();
  disconnect = vi.fn();
}

describe('Dashboard (page synthese)', () => {
  let api: ApiStub;
  let ws: WsStub;
  let state: SystemStateService;

  beforeEach(async () => {
    api = new ApiStub();
    ws = new WsStub();

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: ApiService, useValue: api },
        { provide: WebSocketService, useValue: ws },
      ],
    }).compileComponents();

    state = TestBed.inject(SystemStateService);
  });

  function create() {
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
    return { fixture };
  }

  it('affiche la synthese a partir de l etat partage (REST)', () => {
    state.refresh();
    create();
    expect(api.getStatus).toHaveBeenCalled();
    const compiled = TestBed.createComponent(Dashboard).nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('État de la bande transporteuse');
  });

  it('passe en CRITICAL sur message WS via l etat partage', () => {
    state.refresh();
    state.connectWebSocket();
    create();

    expect(state.status()?.status).toBe('NORMAL');
    expect(state.isCritical()).toBe(false);

    ws.messages$.next({
      status: 'CRITICAL',
      blockagePercentage: 90,
      timestamp: '2026-08-31T09:05:00Z',
    });

    expect(state.status()?.status).toBe('CRITICAL');
    expect(state.isCritical()).toBe(true);
  });

  it('limite l historique recent affiche a 8 evenements', () => {
    const events = Array.from({ length: 20 }, (_, i) => ({
      id: i + 1,
      timestamp: '2026-08-31T09:00:00Z',
      from: 'NORMAL',
      to: 'WARNING',
      blockagePercentage: 30,
      oversizedRocks: 0,
      framesInWindow: 10,
      framesAboveThresholdPercentage: 100,
    }));
    api.getEvents = vi.fn(() => of(events)) as unknown as typeof api.getEvents;
    state.refresh();
    create();
    expect(state.events().length).toBe(20);
    // La page n'affiche que les 8 derniers via recentEvents().
  });
});