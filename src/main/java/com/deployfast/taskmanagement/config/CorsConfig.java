package com.deployfast.taskmanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS pour les requêtes cross-origin.
 * Permet l'accès depuis les origins autorisés avec les headers nécessaires.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Configure les paramètres CORS globaux.
     * 
     * @param registry L'enregistreur de configuration CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:8080", "http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
