package com.deployfast.taskmanagement.service;

import com.deployfast.taskmanagement.entity.User;
import com.deployfast.taskmanagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service pour charger les détails utilisateur.
 * Implémente UserDetailsService pour Spring Security.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charge les détails utilisateur par nom d'utilisateur.
     * 
     * @param username Le nom d'utilisateur
     * @return Les détails utilisateur
     * @throws UsernameNotFoundException Si l'utilisateur n'existe pas
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Utilisateur non trouvé avec le username: {}", username);
                    return new UsernameNotFoundException("Utilisateur non trouvé: " + username);
                });

        log.debug("Utilisateur chargé avec succès: {}", username);
        return user;
    }
}
