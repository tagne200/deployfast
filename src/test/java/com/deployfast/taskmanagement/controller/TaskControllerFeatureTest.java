package com.deployfast.taskmanagement.controller;

import com.deployfast.taskmanagement.dto.CreateTaskRequest;
import com.deployfast.taskmanagement.dto.TaskDto;
import com.deployfast.taskmanagement.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Feature (intégration) pour le contrôleur des tâches.
 * Teste les endpoints REST avec le contexte complet de Spring.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TaskController Feature Tests")
class TaskControllerFeatureTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateTaskRequest createTaskRequest;
    private TaskDto taskDto;

    @BeforeEach
    void setUp() {
        createTaskRequest = CreateTaskRequest.builder()
                .title("Test Task")
                .description("Test Description")
                .status("PENDING")
                .priority("MEDIUM")
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();

        taskDto = TaskDto.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status("PENDING")
                .priority("MEDIUM")
                .userId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Devrait créer une tâche - 201 Created")
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateTaskSuccess() throws Exception {
        // Arrange
        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(taskDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(taskService).createTask(any(CreateTaskRequest.class));
    }

    @Test
    @DisplayName("Devrait retourner 401 sans authentification")
    void testCreateTaskUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait récupérer l'historique des tâches")
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetAllTasksSuccess() throws Exception {
        // Arrange
        Page<TaskDto> taskPage = new PageImpl<>(List.of(taskDto), PageRequest.of(0, 10), 1);
        when(taskService.getAllTasks(any())).thenReturn(taskPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks?page=0&size=10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Task"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(taskService).getAllTasks(any());
    }

    @Test
    @DisplayName("Devrait récupérer une tâche par ID")
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetTaskByIdSuccess() throws Exception {
        // Arrange
        when(taskService.getTaskById(1L)).thenReturn(taskDto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/tasks/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));

        verify(taskService).getTaskById(1L);
    }

    @Test
    @DisplayName("Devrait retourner 400 pour requête invalide")
    @WithMockUser(username = "testuser", roles = "USER")
    void testCreateTaskInvalidRequest() throws Exception {
        // Arrange - Une requête sans titre (obligatoire)
        String invalidPayload = "{\"description\": \"No title\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
