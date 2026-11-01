package com.deployfast.taskmanagement.security;

import com.deployfast.taskmanagement.dto.LoginRequest;
import com.deployfast.taskmanagement.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'authentification JWT.
 * Teste l'enregistrement, la connexion et l'accès aux endpoints protégés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Authentication Security Tests")
class AuthenticationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123!")
                .firstName("New")
                .lastName("User")
                .build();

        loginRequest = LoginRequest.builder()
                .emailOrUsername("newuser@example.com")
                .password("Password123!")
                .build();
    }

    @Test
    @DisplayName("Devrait enregistrer un nouvel utilisateur - 201 Created")
    void testRegisterSuccess() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("newuser"))
                .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Devrait rejeter l'enregistrement avec email existant - 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        // Arrange - Enregistrer d'abord
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Act & Assert - Essayer d'enregistrer avec le même email
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Devrait rejeter la connexion avec mauvais mot de passe - 401 Unauthorized")
    void testLoginInvalidPassword() throws Exception {
        // Arrange - Enregistrer d'abord
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Act & Assert - Essayer une connexion avec mauvais mot de passe
        LoginRequest invalidLogin = LoginRequest.builder()
                .emailOrUsername("newuser@example.com")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait connecter l'utilisateur avec succès - 200 OK")
    void testLoginSuccess() throws Exception {
        // Arrange - Enregistrer d'abord
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Act & Assert - Connexion réussie
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("newuser"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Devrait rejeter les requêtes non authentifiées - 401 Unauthorized")
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait rejeter les requêtes avec token invalide - 401 Unauthorized")
    void testAccessProtectedEndpointWithInvalidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/tasks")
                .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait rejeter un mot de passe faible")
    void testRegisterWeakPassword() throws Exception {
        // Arrange
        RegisterRequest weakPassword = RegisterRequest.builder()
                .username("weakuser")
                .email("weak@example.com")
                .password("weak")  // Password faible
                .firstName("Weak")
                .lastName("User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(weakPassword)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait permettre l'accès à la santé sans authentification")
    void testHealthEndpointNoAuth() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("DeployFast Task Management API is running!"));
    }
}
