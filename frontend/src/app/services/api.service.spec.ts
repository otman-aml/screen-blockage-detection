import { TestBed } from '@angular/core/testing';
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ApiService } from './api.service';
import { environment } from '../environment';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getStatus appelle GET /api/status', () => {
    const body = {
      status: 'NORMAL',
      blockagePercentage: 12.5,
      framesAboveThresholdPercentage: 0,
      oversizedRocks: 0,
      sampleCount: 10,
      windowStart: null,
      analyzedAt: '2026-08-31T10:00:00Z',
    };

    service.getStatus().subscribe((status) => {
      expect(status.status).toBe('NORMAL');
      expect(status.blockagePercentage).toBe(12.5);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/status`);
    expect(req.request.method).toBe('GET');
    req.flush(body);
  });

  it('getEvents appelle GET /api/events', () => {
    service.getEvents().subscribe((events) => {
      expect(events.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/events`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 1,
        from: 'NORMAL',
        to: 'WARNING',
        timestamp: '2026-08-31T10:00:00Z',
        blockagePercentage: 35,
        oversizedRocks: 2,
        framesInWindow: 12,
      },
    ]);
  });

  it('getLatestAnalysis appelle GET /api/detections/latest', () => {
    service.getLatestAnalysis().subscribe((analysis) => {
      expect(analysis.numberOfDetections).toBe(1);
      expect(analysis.detections.length).toBe(1);
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/detections/latest`,
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 7,
      imageWidth: 640,
      imageHeight: 480,
      blockagePercentage: 42,
      analyzedAt: '2026-08-31T10:00:00Z',
      numberOfDetections: 1,
      detections: [
        {
          id: 1,
          classId: 0,
          className: 'rock',
          confidence: 0.9,
          widthCm: 30,
          heightCm: 20,
          oversized: false,
          mask: [
            [1, 1],
            [2, 1],
            [2, 2],
          ],
        },
      ],
    });
  });

  it('latestImageUrl pointe vers /api/detections/latest/image', () => {
    expect(service.latestImageUrl()).toBe(
      `${environment.apiBaseUrl}/api/detections/latest/image`,
    );
  });
});