/**
 * Configuration centralisee du frontend.
 *
 * <p>Les URLs du backend sont configurees ICI, une seule fois
 * (pas de valeurs en dur dispersees dans les composants/services).
 * Ajuster ces valeurs selon l'environnement (localhost / reseau).
 */
export const environment = {
  /** Base URL du backend Spring Boot (REST). */
  apiBaseUrl: 'http://localhost:8080',

  /** Base URL du backend pour la connexion WebSocket (SockJS utilise HTTP). */
  wsBaseUrl: 'http://localhost:8080',

  /** Endpoint SockJS expose par le backend (Phase 5). */
  wsEndpoint: '/ws',

  /** Topic STOMP de diffusion des changements d'etat (Phase 5). */
  statusTopic: '/topic/status',
};
