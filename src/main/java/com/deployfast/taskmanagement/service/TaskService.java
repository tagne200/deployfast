package com.deployfast.taskmanagement.service;

import com.deployfast.taskmanagement.dto.*;
import com.deployfast.taskmanagement.entity.Task;
import com.deployfast.taskmanagement.entity.TaskPriority;
import com.deployfast.taskmanagement.entity.TaskStatus;
import com.deployfast.taskmanagement.entity.User;
import com.deployfast.taskmanagement.exception.ResourceNotFoundException;
import com.deployfast.taskmanagement.exception.BusinessValidationException;
import com.deployfast.taskmanagement.repository.TaskRepository;
import com.deployfast.taskmanagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service pour la gestion des tâches.
 * Implémente la logique métier pour créer, lire, mettre à jour et supprimer des tâches.
 */
@Slf4j
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crée une nouvelle tâche pour l'utilisateur authentifié.
     * 
     * @param request Les données de création de tâche
     * @return La tâche créée en tant que DTO
     * @throws BusinessValidationException Si les données ne sont pas valides
     */
    public TaskDto createTask(CreateTaskRequest request) {
        log.info("Création d'une nouvelle tâche: {}", request.getTitle());

        User currentUser = getCurrentUser();

        // Validation
        validateCreateTaskRequest(request);

        // Création de la tâche
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.valueOf(request.getStatus()))
                .priority(TaskPriority.valueOf(request.getPriority()))
                .dueDate(request.getDueDate())
                .user(currentUser)
                .build();

        task = taskRepository.save(task);
        log.info("Tâche créée avec succès: {} (ID: {})", task.getTitle(), task.getId());

        return convertToTaskDTO(task);
    }

    /**
     * Récupère une tâche par son ID pour l'utilisateur courant.
     * 
     * @param taskId L'ID de la tâche
     * @return La tâche en tant que DTO
     * @throws ResourceNotFoundException Si la tâche n'existe pas ou n'appartient pas à l'utilisateur
     */
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long taskId) {
        User currentUser = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Vérifier que la tâche appartient à l'utilisateur courant
        if (!task.getUser().getId().equals(currentUser.getId())) {
            log.warn("Tentative d'accès non autorisé à la tâche ID: {} par l'utilisateur: {}", taskId, currentUser.getId());
            throw new ResourceNotFoundException("Task", "id", taskId);
        }

        return convertToTaskDTO(task);
    }

    /**
     * Récupère toutes les tâches de l'utilisateur authentifié avec pagination.
     * 
     * @param pageable Les paramètres de pagination
     * @return Page de tâches en tant que DTOs
     */
    @Transactional(readOnly = true)
    public Page<TaskDto> getAllTasks(Pageable pageable) {
        User currentUser = getCurrentUser();
        log.debug("Récupération des tâches pour l'utilisateur: {}", currentUser.getId());

        return taskRepository.findByUserId(currentUser.getId(), pageable)
                .map(this::convertToTaskDTO);
    }

    /**
     * Récupère les tâches de l'utilisateur filtrées par statut.
     * 
     * @param status Le statut à filtrer
     * @param pageable Les paramètres de pagination
     * @return Page de tâches filtrées en tant que DTOs
     * @throws BusinessValidationException Si le statut est invalide
     */
    @Transactional(readOnly = true)
    public Page<TaskDto> getTasksByStatus(String status, Pageable pageable) {
        User currentUser = getCurrentUser();

        try {
            TaskStatus taskStatus = TaskStatus.valueOf(status);
            return taskRepository.findByUserIdAndStatus(currentUser.getId(), taskStatus, pageable)
                    .map(this::convertToTaskDTO);
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Statut de tâche invalide: " + status);
        }
    }

    /**
     * Récupère les tâches de l'utilisateur filtrées par priorité.
     * 
     * @param priority La priorité à filtrer
     * @param pageable Les paramètres de pagination
     * @return Page de tâches filtrées en tant que DTOs
     * @throws BusinessValidationException Si la priorité est invalide
     */
    @Transactional(readOnly = true)
    public Page<TaskDto> getTasksByPriority(String priority, Pageable pageable) {
        User currentUser = getCurrentUser();

        try {
            TaskPriority taskPriority = TaskPriority.valueOf(priority);
            return taskRepository.findByUserIdAndPriority(currentUser.getId(), taskPriority, pageable)
                    .map(this::convertToTaskDTO);
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Priorité de tâche invalide: " + priority);
        }
    }

    /**
     * Recherche les tâches de l'utilisateur par titre (contient).
     * 
     * @param title Le titre (partiel) à rechercher
     * @param pageable Les paramètres de pagination
     * @return Page de tâches correspondantes en tant que DTOs
     */
    @Transactional(readOnly = true)
    public Page<TaskDto> searchTasks(String title, Pageable pageable) {
        User currentUser = getCurrentUser();
        log.debug("Recherche de tâches avec le titre: {}", title);

        return taskRepository.findByUserIdAndTitleContainingIgnoreCase(currentUser.getId(), title, pageable)
                .map(this::convertToTaskDTO);
    }

    /**
     * Met à jour une tâche existante.
     * 
     * @param taskId L'ID de la tâche à mettre à jour
     * @param request Les données de mise à jour
     * @return La tâche mise à jour en tant que DTO
     * @throws ResourceNotFoundException Si la tâche n'existe pas
     * @throws BusinessValidationException Si les données ne sont pas valides
     */
    public TaskDto updateTask(Long taskId, UpdateTaskRequest request) {
        log.info("Mise à jour de la tâche ID: {}", taskId);

        User currentUser = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Vérifier l'accès
        if (!task.getUser().getId().equals(currentUser.getId())) {
            log.warn("Tentative de mise à jour non autorisée de la tâche ID: {} par l'utilisateur: {}", taskId, currentUser.getId());
            throw new ResourceNotFoundException("Task", "id", taskId);
        }

        // Mise à jour des champs si fournis
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            try {
                task.setStatus(TaskStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException ex) {
                throw new BusinessValidationException("Statut de tâche invalide: " + request.getStatus());
            }
        }

        if (request.getPriority() != null) {
            try {
                task.setPriority(TaskPriority.valueOf(request.getPriority()));
            } catch (IllegalArgumentException ex) {
                throw new BusinessValidationException("Priorité de tâche invalide: " + request.getPriority());
            }
        }

        if (request.getDueDate() != null) {
            if (request.getDueDate().isBefore(LocalDateTime.now())) {
                throw new BusinessValidationException("La date limite ne peut pas être dans le passé");
            }
            task.setDueDate(request.getDueDate());
        }

        task = taskRepository.save(task);
        log.info("Tâche mise à jour avec succès: {} (ID: {})", task.getTitle(), task.getId());

        return convertToTaskDTO(task);
    }

    /**
     * Supprime une tâche.
     * 
     * @param taskId L'ID de la tâche à supprimer
     * @throws ResourceNotFoundException Si la tâche n'existe pas
     */
    public void deleteTask(Long taskId) {
        log.info("Suppression de la tâche ID: {}", taskId);

        User currentUser = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Vérifier l'accès
        if (!task.getUser().getId().equals(currentUser.getId())) {
            log.warn("Tentative de suppression non autorisée de la tâche ID: {} par l'utilisateur: {}", taskId, currentUser.getId());
            throw new ResourceNotFoundException("Task", "id", taskId);
        }

        taskRepository.delete(task);
        log.info("Tâche supprimée avec succès: ID {}", taskId);
    }

    /**
     * Récupère l'utilisateur actuellement authentifié.
     * 
     * @return L'utilisateur authentifié
     * @throws BusinessValidationException Si l'utilisateur n'est pas trouvé
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessValidationException("Utilisateur non trouvé"));
    }

    /**
     * Valide les données de création de tâche.
     * 
     * @param request Les données à valider
     * @throws BusinessValidationException Si les données ne sont pas valides
     */
    private void validateCreateTaskRequest(CreateTaskRequest request) {
        try {
            TaskStatus.valueOf(request.getStatus());
            TaskPriority.valueOf(request.getPriority());
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Statut ou priorité de tâche invalide");
        }

        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("La date limite ne peut pas être dans le passé");
        }
    }

    /**
     * Convertit une entité Task en DTO TaskDto.
     * 
     * @param task L'entité Task
     * @return Le DTO TaskDto
     */
    private TaskDto convertToTaskDTO(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .userId(task.getUser().getId())
                .build();
    }
}
