package com.deployfast.taskmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application principale pour le système de gestion des tâches DeployFast.
 * 
 * Cette application implémente une API REST sécurisée avec authentification JWT,
 * gestion complète des tâches et bonnes pratiques de sécurité.
 */
@SpringBootApplication
public class TaskManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagementApplication.class, args);
    }
}
