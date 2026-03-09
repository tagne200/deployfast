package com.deployfast.taskmanagement.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les opérations de mise à jour partielle de tâches.
 * Tous les champs sont optionnels.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskRequest {

    @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    private String status;
    private String priority;
    private LocalDateTime dueDate;
}
