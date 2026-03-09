package com.deployfast.taskmanagement.repository;

import com.deployfast.taskmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'accès aux enregistrements User en base de données.
 * Fournit les méthodes CRUD et des requêtes personnalisées.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Retrouve un utilisateur par son email.
     * 
     * @param email L'email de l'utilisateur
     * @return Optional contenant l'utilisateur s'il existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Retrouve un utilisateur par son nom d'utilisateur.
     * 
     * @param username Le nom d'utilisateur
     * @return Optional contenant l'utilisateur s'il existe
     */
    Optional<User> findByUsername(String username);

    /**
     * Vérifie l'existence d'un utilisateur par email.
     * 
     * @param email L'email à vérifier
     * @return true si l'utilisateur existe, false sinon
     */
    boolean existsByEmail(String email);

    /**
     * Vérifie l'existence d'un utilisateur par nom d'utilisateur.
     * 
     * @param username Le nom d'utilisateur à vérifier
     * @return true si l'utilisateur existe, false sinon
     */
    boolean existsByUsername(String username);

    /**
     * Retrouve un utilisateur par email ou nom d'utilisateur.
     * Utilisé pour la connexion flexible.
     * 
     * @param email L'email de l'utilisateur
     * @param username Le nom d'utilisateur
     * @return Optional contenant l'utilisateur s'il existe
     */
    @Query("SELECT u FROM User u WHERE u.email = :email OR u.username = :username")
    Optional<User> findByEmailOrUsername(String email, String username);
}
