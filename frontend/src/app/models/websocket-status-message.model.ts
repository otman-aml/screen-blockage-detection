import { BlockageStatusValue } from './status.model';

/**
 * Message recu sur /topic/status via WebSocket (Phase 5).
 * Structure : { "status", "blockagePercentage", "timestamp" }.
 * Le frontend affiche exactement ces valeurs sans les recalculer.
 */
export interface WebSocketStatusMessage {
  status: BlockageStatusValue;
  blockagePercentage: number;
  timestamp: string;
}
