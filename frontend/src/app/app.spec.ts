import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { App } from './app';
import { ApiService } from './services/api.service';
import { WebSocketService } from './services/websocket.service';
import { Status } from './models/status.model';
import { WebSocketStatusMessage } from './models/websocket-status-message.model';

class HttpApiStub {
  getStatus = vi.fn(() =>
    of<Status>({
      status: 'NORMAL',
      blockagePercentage: 0,
      framesAboveThresholdPercentage: 0,
      oversizedRocks: 0,
      sampleCount: 0,
      windowStart: null,
      analyzedAt: null,
    }),
  );
  getEvents = vi.fn(() => of([]));
  getLatestAnalysis = vi.fn(() =>
    of({
      id: 1,
      imageWidth: 640,
      imageHeight: 480,
      blockagePercentage: 0,
      analyzedAt: '2026-08-31T09:00:00Z',
      numberOfDetections: 0,
      detections: [],
    }),
  );
  latestImageUrl = vi.fn(() => 'image.png');
  getRecentAnalyses = vi.fn(() => of([]));
}

class HttpWsStub {
  connected = signal(false);
  messages$ = new Subject<WebSocketStatusMessage>();
  connect = vi.fn();
  disconnect = vi.fn();
}

describe('App (shell + routing)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: ApiService, useClass: HttpApiStub },
        { provide: WebSocketService, useClass: HttpWsStub },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('renders the shell (header + sidebar + router-outlet)', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-header')).toBeTruthy();
    expect(compiled.querySelector('app-sidebar')).toBeTruthy();
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });

  it('connecte le WebSocket au demarrage', () => {
    const ws = TestBed.inject(WebSocketService) as unknown as HttpWsStub;
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(ws.connect).toHaveBeenCalled();
  });
});
