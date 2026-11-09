import { BlockageStatusValue } from './status.model';

/**
 * Une transition d'etat historisee par le backend.
 * Reponse de GET /api/events (Phase 4).
 */
export interface StatusEvent {
  id: number;
  from: BlockageStatusValue;
  to: BlockageStatusValue;
  timestamp: string;
  /** Blocage de la DERNIERE analyse (image) au moment de la transition. */
  blockagePercentage: number;
  oversizedRocks: number;
  framesInWindow: number;
  /**
   * INDICATEUR DE DECISION WARNING : part (%) des frames de la fenetre
   * au-dessus du seuil au moment de la transition (fourni par le backend).
   */
  framesAboveThresholdPercentage: number;
}
