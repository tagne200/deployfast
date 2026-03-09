package com.deployfast.taskmanagement.repository;

import com.deployfast.taskmanagement.entity.Task;
import com.deployfast.taskmanagement.entity.TaskPriority;
import com.deployfast.taskmanagement.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour l'accès aux enregistrements Task en base de données.
 * Fournit les méthodes CRUD, pagination et requêtes personnalisées por filtrage.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Retrouve toutes les tâches d'un utilisateur avec pagination.
     * 
     * @param userId L'ID de l'utilisateur
     * @param pageable Les paramètres de pagination
     * @return Page de tâches paginées
     */
    Page<Task> findByUserId(Long userId, Pageable pageable);

    /**
     * Retrouve les tâches d'un utilisateur filtrées par statut.
     * 
     * @param userId L'ID de l'utilisateur
     * @param status Le statut de la tâche
     * @param pageable Les paramètres de pagination
     * @return Page de tâches filtrées
     */
    Page<Task> findByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    /**
     * Retrouve les tâches d'un utilisateur filtrées par priorité.
     * 
     * @param userId L'ID de l'utilisateur
     * @param priority La priorité de la tâche
     * @param pageable Les paramètres de pagination
     * @return Page de tâches filtrées
     */
    Page<Task> findByUserIdAndPriority(Long userId, TaskPriority priority, Pageable pageable);

    /**
     * Retrouve les tâches d'un utilisateur filtrées par titre (contient).
     * 
     * @param userId L'ID de l'utilisateur
     * @param title Le titre (partiel) à rechercher
     * @param pageable Les paramètres de pagination
     * @return Page de tâches correspondantes
     */
    Page<Task> findByUserIdAndTitleContainingIgnoreCase(Long userId, String title, Pageable pageable);

    /**
     * Retrouve les tâches d'un utilisateur avec date de fin dépassée et statut non complété.
     * 
     * @param userId L'ID de l'utilisateur
     * @param now La date actuelle
     * @param status Le statut à exclure
     * @return Liste des tâches en retard
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.dueDate < :now AND t.status != :status")
    List<Task> findOverdueTasks(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("status") TaskStatus status);

    /**
     * Compte le nombre de tâches par statut pour un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @param status Le statut à compter
     * @return Le nombre de tâches avec ce statut
     */
    long countByUserIdAndStatus(Long userId, TaskStatus status);

    /**
     * Retrouve les tâches d'un utilisateur avec filtrage combiné.
     * 
     * @param userId L'ID de l'utilisateur
     * @param status Le statut à filtrer (optionnel)
     * @param priority La priorité à filtrer (optionnel)
     * @param pageable Les paramètres de pagination
     * @return Page de tâches filtrées
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority)")
    Page<Task> searchTasks(@Param("userId") Long userId,
                           @Param("status") TaskStatus status,
                           @Param("priority") TaskPriority priority,
                           Pageable pageable);
}
