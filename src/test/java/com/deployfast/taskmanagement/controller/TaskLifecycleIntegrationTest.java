package com.deployfast.taskmanagement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours complet avec un vrai token JWT et la base de test :
 * inscription, création, lecture, modification et suppression d'une tâche.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task Lifecycle Integration Tests")
class TaskLifecycleIntegrationTest {

    private static final String REGISTER = """
            {"username":"parcours","email":"parcours@example.com","password":"Password123!",
             "firstName":"Par","lastName":"Cours"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    @DisplayName("Devrait gérer une tâche de bout en bout avec un token JWT")
    void testTaskLifecycle() throws Exception {
        String token = registerAndGetToken();

        String created = mockMvc.perform(post("/tasks")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Ma tache\",\"description\":\"Texte\",\"status\":\"PENDING\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode task = objectMapper.readTree(created);
        long id = task.get("id").asLong();

        mockMvc.perform(get("/tasks/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ma tache"));

        mockMvc.perform(put("/tasks/" + id)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Titre modifie\",\"status\":\"COMPLETED\",\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titre modifie"));

        mockMvc.perform(get("/tasks/filter/priority").param("priority", "LOW").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/tasks/" + id).header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/" + id).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Devrait refuser un token invalide - 401")
    void testInvalidToken() throws Exception {
        mockMvc.perform(get("/tasks").header("Authorization", "Bearer token-invalide"))
                .andExpect(status().isUnauthorized());
    }
}
