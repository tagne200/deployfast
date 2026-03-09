package com.deployfast.taskmanagement.entity;

/**
 * Énumération des statuts possibles pour une tâche.
 */
public enum TaskStatus {
    PENDING("À faire"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminée"),
    CANCELLED("Annulée");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
