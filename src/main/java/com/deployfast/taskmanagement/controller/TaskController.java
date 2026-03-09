package com.deployfast.taskmanagement.controller;

import com.deployfast.taskmanagement.dto.CreateTaskRequest;
import com.deployfast.taskmanagement.dto.UpdateTaskRequest;
import com.deployfast.taskmanagement.dto.TaskDto;
import com.deployfast.taskmanagement.dto.PageResponse;
import com.deployfast.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des tâches.
 * Implémente les opérations CRUD avec pagination et filtrage.
 * 
 * Tous les endpoints nécessitent une authentification JWT.
 */
@Slf4j
@RestController
@RequestMapping("/tasks")
@PreAuthorize("isAuthenticated()")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Crée une nouvelle tâche.
     * 
     * @param request Les données de création
     * @return La tâche créée
     */
    @PostMapping
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody CreateTaskRequest request) {
        log.info("Création d'une nouvelle tâche: {}", request.getTitle());
        TaskDto taskDto = taskService.createTask(request);
        return new ResponseEntity<>(taskDto, HttpStatus.CREATED);
    }

    /**
     * Récupère toutes les tâches avec pagination.
     * 
     * @param page Le numéro de page (défaut: 0)
     * @param size La taille de la page (défaut: 10)
     * @return Page de tâches
     */
    @GetMapping
    public ResponseEntity<PageResponse<TaskDto>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Récupération de toutes les tâches - Page: {}, Size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskDto> tasks = taskService.getAllTasks(pageable);

        PageResponse<TaskDto> response = PageResponse.<TaskDto>builder()
                .content(tasks.getContent())
                .pageNumber(tasks.getNumber())
                .pageSize(tasks.getSize())
                .totalElements(tasks.getTotalElements())
                .totalPages(tasks.getTotalPages())
                .first(tasks.isFirst())
                .last(tasks.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Récupère une tâche par son ID.
     * 
     * @param taskId L'ID de la tâche
     * @return La tâche trouvée
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long taskId) {
        log.debug("Récupération de la tâche ID: {}", taskId);
        TaskDto taskDto = taskService.getTaskById(taskId);
        return ResponseEntity.ok(taskDto);
    }

    /**
     * Filtre les tâches par statut avec pagination.
     * 
     * @param status Le statut à filtrer
     * @param page Le numéro de page
     * @param size La taille de la page
     * @return Page de tâches filtrées
     */
    @GetMapping("/filter/status")
    public ResponseEntity<PageResponse<TaskDto>> getTasksByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Filtrage des tâches par statut: {}", status);

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskDto> tasks = taskService.getTasksByStatus(status, pageable);

        PageResponse<TaskDto> response = PageResponse.<TaskDto>builder()
                .content(tasks.getContent())
                .pageNumber(tasks.getNumber())
                .pageSize(tasks.getSize())
                .totalElements(tasks.getTotalElements())
                .totalPages(tasks.getTotalPages())
                .first(tasks.isFirst())
                .last(tasks.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Filtre les tâches par priorité avec pagination.
     * 
     * @param priority La priorité à filtrer
     * @param page Le numéro de page
     * @param size La taille de la page
     * @return Page de tâches filtrées
     */
    @GetMapping("/filter/priority")
    public ResponseEntity<PageResponse<TaskDto>> getTasksByPriority(
            @RequestParam String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Filtrage des tâches par priorité: {}", priority);

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskDto> tasks = taskService.getTasksByPriority(priority, pageable);

        PageResponse<TaskDto> response = PageResponse.<TaskDto>builder()
                .content(tasks.getContent())
                .pageNumber(tasks.getNumber())
                .pageSize(tasks.getSize())
                .totalElements(tasks.getTotalElements())
                .totalPages(tasks.getTotalPages())
                .first(tasks.isFirst())
                .last(tasks.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Recherche les tâches par titre (contient).
     * 
     * @param title Le titre à rechercher
     * @param page Le numéro de page
     * @param size La taille de la page
     * @return Page de tâches correspondantes
     */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<TaskDto>> searchTasks(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Recherche de tâches avec le titre: {}", title);

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskDto> tasks = taskService.searchTasks(title, pageable);

        PageResponse<TaskDto> response = PageResponse.<TaskDto>builder()
                .content(tasks.getContent())
                .pageNumber(tasks.getNumber())
                .pageSize(tasks.getSize())
                .totalElements(tasks.getTotalElements())
                .totalPages(tasks.getTotalPages())
                .first(tasks.isFirst())
                .last(tasks.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour une tâche existante.
     * 
     * @param taskId L'ID de la tâche
     * @param request Les données de mise à jour
     * @return La tâche mise à jour
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDto> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        log.info("Mise à jour de la tâche ID: {}", taskId);
        TaskDto taskDto = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(taskDto);
    }

    /**
     * Supprime une tâche.
     * 
     * @param taskId L'ID de la tâche
     * @return Réponse sans contenu
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        log.info("Suppression de la tâche ID: {}", taskId);
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
