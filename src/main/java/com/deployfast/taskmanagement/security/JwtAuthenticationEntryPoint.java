package com.deployfast.taskmanagement.security;

import com.deployfast.taskmanagement.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Point d'entrée pour les erreurs d'authentification JWT.
 * Retourne une réponse JSON structurée lors d'une tentative d'accès non autorisée.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Commence une erreur d'authentification en retournant une réponse HTTP 401.
     * 
     * @param request Le requête HTTP
     * @param response La réponse HTTP
     * @param authException L'exception d'authentification
     * @throws IOException Si une erreur IO se produit
     * @throws ServletException Si une erreur servlet se produit
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        log.error("Erreur d'authentification: {}", authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final GlobalExceptionHandler.ErrorResponse errorResponse = GlobalExceptionHandler.ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error("Non autorisé")
                .message("Accès refusé - token JWT manquant ou invalide")
                .path(request.getServletPath())
                .errorCode("AUTHENTICATION_REQUIRED")
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
