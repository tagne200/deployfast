package com.deployfast.taskmanagement.service;

import com.deployfast.taskmanagement.dto.LoginRequest;
import com.deployfast.taskmanagement.dto.RegisterRequest;
import com.deployfast.taskmanagement.dto.AuthResponse;
import com.deployfast.taskmanagement.entity.User;
import com.deployfast.taskmanagement.entity.UserRole;
import com.deployfast.taskmanagement.exception.ResourceAlreadyExistsException;
import com.deployfast.taskmanagement.exception.BusinessValidationException;
import com.deployfast.taskmanagement.repository.UserRepository;
import com.deployfast.taskmanagement.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le service d'authentification AuthService.
 * Teste l'enregistrement, la connexion et la validation du mot de passe.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, authenticationManager, jwtTokenProvider, passwordEncoder);

        // Initialiser les données de test
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123!")
                .firstName("Test")
                .lastName("User")
                .build();

        loginRequest = LoginRequest.builder()
                .emailOrUsername("test@example.com")
                .password("Password123!")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Devrait enregistrer un nouvel utilisateur avec succès")
    void testRegisterSuccess() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        when(jwtTokenProvider.generateAccessToken(authentication)).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(authentication)).thenReturn("refreshToken");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(86400000L);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("testuser", response.getUser().getUsername());

        verify(userRepository, times(1)).save(any(User.class));
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    @DisplayName("Devrait rejeter l'enregistrement si l'email existe déjà")
    void testRegisterEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait rejeter l'enregistrement si le username existe déjà")
    void testRegisterUsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait rejeter un mot de passe faible")
    void testRegisterWeakPassword() {
        // Arrange
        RegisterRequest weakRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("weak")  // Mot de passe faible
                .firstName("Test")
                .lastName("User")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessValidationException.class, () -> {
            authService.register(weakRequest);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait connecter l'utilisateur avec succès")
    void testLoginSuccess() {
        // Arrange
        when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        when(jwtTokenProvider.generateAccessToken(authentication)).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(authentication)).thenReturn("refreshToken");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(86400000L);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        verify(userRepository, times(1)).findByEmailOrUsername(anyString(), anyString());
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    @DisplayName("Devrait rejeter la connexion avec mauvais identifiants")
    void testLoginInvalidCredentials() {
        // Arrange
        when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(authenticationManager, never()).authenticate(any());
    }
}
