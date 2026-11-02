package com.deployfast.taskmanagement.service;

import com.deployfast.taskmanagement.dto.CreateTaskRequest;
import com.deployfast.taskmanagement.dto.TaskDto;
import com.deployfast.taskmanagement.dto.UpdateTaskRequest;
import com.deployfast.taskmanagement.entity.Task;
import com.deployfast.taskmanagement.entity.TaskPriority;
import com.deployfast.taskmanagement.entity.TaskStatus;
import com.deployfast.taskmanagement.entity.User;
import com.deployfast.taskmanagement.entity.UserRole;
import com.deployfast.taskmanagement.exception.ResourceNotFoundException;
import com.deployfast.taskmanagement.exception.BusinessValidationException;
import com.deployfast.taskmanagement.repository.TaskRepository;
import com.deployfast.taskmanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le service de gestion des tâches TaskService.
 * Teste la création, lecture, mise à jour et suppression des tâches.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    private TaskService taskService;

    private User testUser;
    private Task testTask;
    private CreateTaskRequest createTaskRequest;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createTaskRequest = CreateTaskRequest.builder()
                .title("New Task")
                .description("New Description")
                .status("PENDING")
                .priority("MEDIUM")
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();

        // Setup SecurityContext
        mockSecurityContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("testuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Devrait créer une tâche avec succès")
    void testCreateTaskSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // Act
        TaskDto result = taskService.createTask(createTaskRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals("PENDING", result.getStatus());
        assertEquals("MEDIUM", result.getPriority());

        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Devrait rejeter une tâche avec une date limite dans le passé")
    void testCreateTaskInvalidDueDate() {
        // Arrange
        CreateTaskRequest invalidRequest = CreateTaskRequest.builder()
                .title("Invalid Task")
                .description("Description")
                .status("PENDING")
                .priority("MEDIUM")
                .dueDate(LocalDateTime.now().minusDays(1))  // Date dans le passé
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BusinessValidationException.class, () -> {
            taskService.createTask(invalidRequest);
        });

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait récupérer une tâche par ID")
    void testGetTaskByIdSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // Act
        TaskDto result = taskService.getTaskById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Task", result.getTitle());

        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Devrait lever une exception si la tâche n'existe pas")
    void testGetTaskByIdNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            taskService.getTaskById(999L);
        });
    }

    @Test
    @DisplayName("Devrait récupérer l'historique des tâches avec pagination")
    void testGetAllTasksSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Page<Task> taskPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByUserId(1L, PageRequest.of(0, 10))).thenReturn(taskPage);

        // Act
        Page<TaskDto> result = taskService.getAllTasks(PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Task", result.getContent().get(0).getTitle());

        verify(taskRepository, times(1)).findByUserId(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("Devrait mettre à jour une tâche avec succès")
    void testUpdateTaskSuccess() {
        // Arrange
        UpdateTaskRequest updateRequest = UpdateTaskRequest.builder()
                .title("Updated Task")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .build();

        Task updatedTask = Task.builder()
                .id(1L)
                .title("Updated Task")
                .description("Test Description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // Act
        TaskDto result = taskService.updateTask(1L, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Task", result.getTitle());
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals("HIGH", result.getPriority());

        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Devrait supprimer une tâche avec succès")
    void testDeleteTaskSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // Act
        taskService.deleteTask(1L);

        // Assert
        verify(taskRepository, times(1)).delete(testTask);
    }

    @Test
    @DisplayName("Devrait rechercher des tâches par titre")
    void testSearchTasksSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Page<Task> taskPage = new PageImpl<>(List.of(testTask), PageRequest.of(0, 10), 1);
        when(taskRepository.findByUserIdAndTitleContainingIgnoreCase(1L, "Test", PageRequest.of(0, 10)))
                .thenReturn(taskPage);

        // Act
        Page<TaskDto> result = taskService.searchTasks("Test", PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        verify(taskRepository, times(1)).findByUserIdAndTitleContainingIgnoreCase(anyLong(), anyString(), any());
    }
}
