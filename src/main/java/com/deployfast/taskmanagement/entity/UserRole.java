package com.deployfast.taskmanagement.entity;

/**
 * Énumération des rôles utilisateurs disponibles dans l'application.
 */
public enum UserRole {
    ADMIN("Administrateur - Accès complet"),
    USER("Utilisateur standard - Accès limité");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
