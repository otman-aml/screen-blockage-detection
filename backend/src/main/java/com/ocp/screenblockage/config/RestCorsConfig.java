package com.ocp.screenblockage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Configuration CORS de l'API REST (Phase 6).
 *
 * <p>Permet au dashboard Angular (dev sur {@code http://localhost:4200}) de
 * consommer les endpoints REST {@code /api/**} sans être bloqué par le
 * navigateur. Portée strictement restreinte aux origines de développement
 * du frontend — aucune origine sauvage {@code *} sur l'API.
 *
 * <p>Note : un simple bean {@code CorsConfigurationSource} ne s'applique pas
 * aux contrôleurs Spring MVC (il n'activerait le filtre que sous certaines
 * conditions) ; l'approche idiomatique pour les {@code @RestController} est
 * un {@link WebMvcConfigurer#addCorsMappings}.
 */
@Configuration
public class RestCorsConfig implements WebMvcConfigurer {

    /** Origines du frontend Angular en développement. */
    private static final List<String> ALLOWED_ORIGINS =
            List.of("http://localhost:4200", "http://127.0.0.1:4200");

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(ALLOWED_ORIGINS.toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}