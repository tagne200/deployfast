package com.deployfast.taskmanagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les opérations de création/mise à jour de tâches.
 * Valide strictement les données entrantes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    @NotBlank(message = "Le titre de la tâche est requis")
    @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    @NotNull(message = "Le statut est requis")
    private String status;

    @NotNull(message = "La priorité est requise")
    private String priority;

    @Future(message = "La date limite doit être dans le futur")
    private LocalDateTime dueDate;
}
