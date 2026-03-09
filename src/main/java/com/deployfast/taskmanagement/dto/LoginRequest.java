package com.deployfast.taskmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'authentification de l'utilisateur (connexion).
 * Valide les paramètres d'authentification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "L'email ou le nom d'utilisateur est requis")
    @Size(min = 3, max = 100, message = "L'email ou le nom d'utilisateur doit contenir entre 3 et 100 caractères")
    private String emailOrUsername;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, max = 128, message = "Le mot de passe doit contenir entre 8 et 128 caractères")
    private String password;
}
