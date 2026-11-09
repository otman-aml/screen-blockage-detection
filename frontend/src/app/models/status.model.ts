/**
 * Etats metier produits exclusivement par le BACKEND (Phase 4).
 * Le frontend affiche ces valeurs telles quelles : il ne les recalcule jamais.
 */
export type BlockageStatusValue = 'NORMAL' | 'WARNING' | 'CRITICAL';

/**
 * Reponse de GET /api/status (backend Phase 4).
 */
export interface Status {
  status: BlockageStatusValue;
  blockagePercentage: number;
  framesAboveThresholdPercentage: number;
  oversizedRocks: number;
  sampleCount: number;
  windowStart: string | null;
  analyzedAt: string | null;
}
