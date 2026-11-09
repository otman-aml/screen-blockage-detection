import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environment';
import { Status } from '../models/status.model';
import { StatusEvent } from '../models/event.model';
import { LatestAnalysis, RecentAnalysis } from '../models/detection.model';

/**
 * Service HTTP vers le backend Spring Boot.
 *
 * <p>Les URLs viennent de {@link environment} (une seule source de
 * configuration, jamais de valeurs en dur dans les composants).
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  /** GET /api/status — etat metier courant (decision du backend). */
  getStatus(): Observable<Status> {
    return this.http.get<Status>(`${environment.apiBaseUrl}/api/status`);
  }

  /** GET /api/events — historique des transitions d'etat. */
  getEvents(): Observable<StatusEvent[]> {
    return this.http.get<StatusEvent[]>(`${environment.apiBaseUrl}/api/events`);
  }

  /** GET /api/detections/latest — derniere analyse avec masques. */
  getLatestAnalysis(): Observable<LatestAnalysis> {
    return this.http.get<LatestAnalysis>(
      `${environment.apiBaseUrl}/api/detections/latest`,
    );
  }

  /** URL de l'image de la derniere analyse (pour <img src>). */
  latestImageUrl(): string {
    return `${environment.apiBaseUrl}/api/detections/latest/image`;
  }

  /** POST /api/detections — analyse d'une image selectionnee (Phase 7). */
  analyzeImage(file: File): Observable<LatestAnalysis> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<LatestAnalysis>(
      `${environment.apiBaseUrl}/api/detections`,
      form,
    );
  }

  /** GET /api/detections/recent — historique recent des analyses (Phase 7). */
  getRecentAnalyses(): Observable<RecentAnalysis[]> {
    return this.http.get<RecentAnalysis[]>(
      `${environment.apiBaseUrl}/api/detections/recent`,
    );
  }
}
