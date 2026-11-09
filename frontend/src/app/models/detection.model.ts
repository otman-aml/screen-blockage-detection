import { BlockageStatusValue } from './status.model';

/**
 * Une roche detectee par le ai-service (via le backend).
 * Le polygone `mask` est une liste de points [x, y] dans l'espace de
 * l'image originale (reseau d'entree de YOLOv8-Seg).
 */
export interface Detection {
  id: number;
  classId: number;
  className: string;
  confidence: number;
  widthCm: number;
  heightCm: number;
  oversized: boolean;
  mask: number[][];
}

/**
 * Derniere analyse avec masques : reponse de GET /api/detections/latest
 * (backend Phase 6).
 */
export interface LatestAnalysis {
  id: number;
  imageWidth: number;
  imageHeight: number;
  blockagePercentage: number;
  analyzedAt: string;
  numberOfDetections: number;
  detections: Detection[];
}

/**
 * Resume d'une analyse passee : reponse de GET /api/detections/recent
 * (backend Phase 7 : historique recent pour KPI, graphiques et
 * historique des analyses).
 *
 * `status` est la classification de CETTE image calculee par le BACKEND
 * (seuils configures) : le frontend l'affiche sans la recalculer.
 */
export interface RecentAnalysis {
  id: number;
  analyzedAt: string;
  blockagePercentage: number;
  numberOfDetections: number;
  status: BlockageStatusValue;
}

