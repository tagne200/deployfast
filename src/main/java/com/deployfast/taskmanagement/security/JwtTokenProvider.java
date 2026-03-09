package com.deployfast.taskmanagement.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Composant pour la génération et validation des tokens JWT.
 * Gère la création des tokens d'accès et de rafraîchissement avec sécurité renforcée.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    /**
     * Génère un token JWT d'accès basé sur l'authentification.
     * 
     * @param authentication L'objet Authentication Spring Security
     * @return Le token JWT signé
     */
    public String generateAccessToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "access")
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Génère un token JWT de rafraîchissement pour renew le token d'accès.
     * 
     * @param authentication L'objet Authentication Spring Security
     * @return Le token JWT de rafraîchissement signé
     */
    public String generateRefreshToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "refresh")
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extrait le nom d'utilisateur du token JWT.
     * 
     * @param token Le token JWT
     * @return Le nom d'utilisateur
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Valide le token JWT.
     * 
     * @param token Le token JWT à valider
     * @return true si le token est valide, false sinon
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Erreur de sécurité JWT: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Token JWT invalide: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT expiré: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT non supporté: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("Claims JWT vide: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Retourne le temps d'expiration du token d'accès en millisecondes.
     * 
     * @return Le temps d'expiration en ms
     */
    public long getAccessTokenExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Obtient la clé secrète pour signer les tokens.
     * Assure une clé suffisamment longue pour HS512.
     * 
     * @return La clé secrète
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
