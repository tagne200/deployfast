package com.deployfast.taskmanagement.controller;

import com.deployfast.taskmanagement.dto.TaskDto;
import com.deployfast.taskmanagement.dto.UpdateTaskRequest;
import com.deployfast.taskmanagement.exception.BusinessValidationException;
import com.deployfast.taskmanagement.exception.ResourceNotFoundException;
import com.deployfast.taskmanagement.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests des routes de filtrage, de recherche, de modification et de suppression,
 * ainsi que de la conversion des erreurs métier en réponses HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser", roles = "USER")
@DisplayName("TaskController Endpoints Tests")
class TaskControllerEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private TaskDto taskDto;

    @BeforeEach
    void setUp() {
        taskDto = TaskDto.builder().id(1L).title("Test Task").status("PENDING").priority("HIGH").build();
    }

    @Test
    @DisplayName("Devrait filtrer les tâches par statut")
    void testFilterByStatus() throws Exception {
        when(taskService.getTasksByStatus(eq("PENDING"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taskDto)));

        mockMvc.perform(get("/tasks/filter/status").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Devrait filtrer les tâches par priorité")
    void testFilterByPriority() throws Exception {
        when(taskService.getTasksByPriority(eq("HIGH"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taskDto)));

        mockMvc.perform(get("/tasks/filter/priority").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"));
    }

    @Test
    @DisplayName("Devrait rechercher les tâches par titre")
    void testSearchByTitle() throws Exception {
        when(taskService.searchTasks(eq("Test"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taskDto)));

        mockMvc.perform(get("/tasks/search").param("title", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Task"));
    }

    @Test
    @DisplayName("Devrait modifier une tâche")
    void testUpdateTask() throws Exception {
        UpdateTaskRequest request = UpdateTaskRequest.builder().title("Nouveau titre").build();
        when(taskService.updateTask(eq(1L), any(UpdateTaskRequest.class))).thenReturn(taskDto);

        mockMvc.perform(put("/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("Devrait refuser un titre trop court - 400")
    void testUpdateTaskInvalidTitle() throws Exception {
        UpdateTaskRequest request = UpdateTaskRequest.builder().title("a").build();

        mockMvc.perform(put("/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait supprimer une tâche - 204")
    void testDeleteTask() throws Exception {
        mockMvc.perform(delete("/tasks/1")).andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    @Test
    @DisplayName("Devrait retourner 404 si la tâche est introuvable")
    void testTaskNotFound() throws Exception {
        when(taskService.getTaskById(99L)).thenThrow(new ResourceNotFoundException("Task", "id", 99L));

        mockMvc.perform(get("/tasks/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait retourner 400 pour un statut invalide")
    void testInvalidStatus() throws Exception {
        when(taskService.getTasksByStatus(eq("INCONNU"), any(Pageable.class)))
                .thenThrow(new BusinessValidationException("Statut de tâche invalide: INCONNU"));

        mockMvc.perform(get("/tasks/filter/status").param("status", "INCONNU"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 500 sans détail technique pour une erreur inattendue")
    void testUnexpectedError() throws Exception {
        doThrow(new IllegalStateException("détail interne")).when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Une erreur inattendue s'est produite"));
    }
}
