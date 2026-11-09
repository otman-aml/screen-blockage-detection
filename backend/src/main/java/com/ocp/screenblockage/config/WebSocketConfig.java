package com.ocp.screenblockage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket/STOMP (Phase 5).
 *
 * <p>Aucune logique metier : uniquement un relais du backend -> client.
 * La decision d'etat reste dans {@code BlockageEvaluationService}.
 *
 * <pre>
 * - endpoint SockJS   : /ws
 * - broker simple     : /topic
 * - destination       : /topic/status
 * </pre>
 *
 * <p>Le CORS de l'API REST {@code /api/**} est géré séparément par
 * {@link RestCorsConfig} (Phase 6).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Endpoint SockJS expose sur /ws (CORS pour le test + Angular). */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    /** Broker simple Spring -> /topic (pub/sub pour /topic/status). */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
