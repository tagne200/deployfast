package com.deployfast.taskmanagement.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration de sécurité Spring Security.
 * Implémente l'authentification JWT sans état (stateless) avec CSRF et protections XSS.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true,
    prePostEnabled = true
)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    /**
     * Filtre JWT pour traiter les tokens dans chaque requête.
     * 
     * @return Le filtre JWT
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * Encodeur de mot de passe utilisant BCrypt avec force 12.
     * BCrypt fournit une protection robuste contre les attaques brute-force.
     * 
     * @return L'encodeur BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Manager d'authentification pour traiter les authentifications.
     * 
     * @param config La configuration d'authentification
     * @return Le manager d'authentification
     * @throws Exception Si une erreur se produit
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Chaîne de filtres de sécurité HTTP.
     * Configura les règles d'accès, CSRF, CORS, XSS et autres protections.
     * 
     * @param http La configuration http sécurité
     * @return La chaîne de filtres
     * @throws Exception Si une erreur se produit
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configuration de session: Stateless (sans cookies de session)
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

            // Configuration CORS pour les requêtes cross-origin
            .cors()
            .and()

            // Configuration CSRF: Désactivée pour les APIs REST avec JWT
            .csrf()
            .disable()

            // Configuration des autorisations d'accès
            .authorizeHttpRequests(authz -> authz
                // Endpoints publics (sans authentification)
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // Endpoints Admin
                .requestMatchers(HttpMethod.GET, "/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/admin/**").hasRole("ADMIN")

                // Endpoints User (authentifiés)
                .requestMatchers("/tasks/**").authenticated()
                .requestMatchers("/users/**").authenticated()

                // Tous les autres requêtes nécessitent l'authentification
                .anyRequest().authenticated()
            )

            // Gestionnaire d'erreurs d'authentification
            .exceptionHandling()
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()

            // Headers de sécurité
            .headers()
            .frameOptions().sameOrigin() // Permet les frames du même domaine (H2 console)
            .contentSecurityPolicy("default-src 'self'") // Protection XSS
            .and()
            .and()

            // Ajout du filtre JWT avant le filtre UsernamePassword
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
