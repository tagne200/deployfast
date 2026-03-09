package com.deployfast.taskmanagement.controller;

import com.deployfast.taskmanagement.dto.LoginRequest;
import com.deployfast.taskmanagement.dto.RegisterRequest;
import com.deployfast.taskmanagement.dto.AuthResponse;
import com.deployfast.taskmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'authentification.
 * Gère l'enregistrement, la connexion et la gestion des tokens JWT.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Enregistre un nouvel utilisateur.
     * 
     * @param registerRequest Les données d'enregistrement
     * @return La réponse d'authentification avec tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Nouvelle demande d'enregistrement pour: {}", registerRequest.getUsername());
        AuthResponse authResponse = authService.register(registerRequest);
        return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }

    /**
     * Connexion d'un utilisateur.
     * 
     * @param loginRequest Les données de connexion
     * @return La réponse d'authentification avec tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Nouvelle demande de connexion pour: {}", loginRequest.getEmailOrUsername());
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Point de terminaison de contrôle de santé.
     * Accessible sans authentification.
     * 
     * @return Message de santé
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("DeployFast Task Management API is running!");
    }
}
