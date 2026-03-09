package com.deployfast.taskmanagement.service;

import com.deployfast.taskmanagement.dto.LoginRequest;
import com.deployfast.taskmanagement.dto.RegisterRequest;
import com.deployfast.taskmanagement.dto.AuthResponse;
import com.deployfast.taskmanagement.dto.UserDto;
import com.deployfast.taskmanagement.entity.User;
import com.deployfast.taskmanagement.entity.UserRole;
import com.deployfast.taskmanagement.exception.ResourceAlreadyExistsException;
import com.deployfast.taskmanagement.exception.BusinessValidationException;
import com.deployfast.taskmanagement.repository.UserRepository;
import com.deployfast.taskmanagement.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'authentification.
 * Gère l'enregistrement, la connexion et la gestion des tokens JWT.
 */
@Slf4j
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                      AuthenticationManager authenticationManager,
                      JwtTokenProvider jwtTokenProvider,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Enregistre un nouvel utilisateur dans le système.
     * Valide les données et vérifie les doublons avant création.
     * 
     * @param registerRequest Les données d'enregistrement
     * @return La réponse d'authentification avec tokens
     * @throws ResourceAlreadyExistsException Si email ou username existe déjà
     * @throws BusinessValidationException Si les données ne sont pas valides
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Enregistrement d'un nouvel utilisateur: {}", registerRequest.getUsername());

        // Validation des doublons
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Tentative d'enregistrement avec email existant: {}", registerRequest.getEmail());
            throw new ResourceAlreadyExistsException("User", "email", registerRequest.getEmail());
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Tentative d'enregistrement avec username existant: {}", registerRequest.getUsername());
            throw new ResourceAlreadyExistsException("User", "username", registerRequest.getUsername());
        }

        // Validation sécurité mot de passe
        validatePasswordStrength(registerRequest.getPassword());

        // Création de l'utilisateur
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .role(UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("Nouvel utilisateur enregistré avec succès: {} (ID: {})", user.getUsername(), user.getId());

        // Authentification automatique après enregistrement
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword()
                )
        );

        return generateAuthResponse(authentication, user);
    }

    /**
     * Authentifie un utilisateur et génère les tokens JWT.
     * 
     * @param loginRequest Les données de connexion
     * @return La réponse d'authentification avec tokens
     * @throws BadCredentialsException Si les identifiants sont incorrects
     */
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Tentative de connexion pour: {}", loginRequest.getEmailOrUsername());

        try {
            // Essayer d'authentifier avec le username ou l'email
            User user = userRepository.findByEmailOrUsername(
                    loginRequest.getEmailOrUsername(), 
                    loginRequest.getEmailOrUsername()
            ).orElseThrow(() -> {
                log.warn("Utilisateur non trouvé: {}", loginRequest.getEmailOrUsername());
                return new BadCredentialsException("Email, nom d'utilisateur ou mot de passe incorrect");
            });

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            log.info("Connexion réussie pour: {}", user.getUsername());
            return generateAuthResponse(authentication, user);

        } catch (BadCredentialsException ex) {
            log.warn("Authentification échouée pour: {}", loginRequest.getEmailOrUsername());
            throw new BadCredentialsException("Email, nom d'utilisateur ou mot de passe incorrect");
        }
    }

    /**
     * Valide la force du mot de passe.
     * Doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial.
     * 
     * @param password Le mot de passe à valider
     * @throws BusinessValidationException Si le mot de passe est faible
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessValidationException("Le mot de passe doit contenir au moins 8 caractères", "WEAK_PASSWORD");
        }

        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[@$!%*?&].*");

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
            throw new BusinessValidationException(
                    "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial",
                    "WEAK_PASSWORD"
            );
        }
    }

    /**
     * Génère la réponse d'authentification avec tokens JWT.
     * 
     * @param authentication L'objet Authentication Spring Security
     * @param user L'utilisateur authentifié
     * @return La réponse d'authentification complète
     */
    private AuthResponse generateAuthResponse(Authentication authentication, User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        UserDto userDto = convertToUserDTO(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs())
                .user(userDto)
                .build();
    }

    /**
     * Convertit une entité User en DTO UserDto.
     * 
     * @param user L'entité User
     * @return Le DTO UserDto
     */
    private UserDto convertToUserDTO(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
