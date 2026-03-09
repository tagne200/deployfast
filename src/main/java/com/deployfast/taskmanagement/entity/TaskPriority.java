package com.deployfast.taskmanagement.entity;

/**
 * Énumération des niveaux de priorité pour une tâche.
 */
public enum TaskPriority {
    LOW("Basse", 1),
    MEDIUM("Moyenne", 2),
    HIGH("Haute", 3),
    CRITICAL("Critique", 4);

    private final String label;
    private final int level;

    TaskPriority(String label, int level) {
        this.label = label;
        this.level = level;
    }

    public String getLabel() {
        return label;
    }

    public int getLevel() {
        return level;
    }
}
