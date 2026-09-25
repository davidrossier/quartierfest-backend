package ch.quartierfest.backend.person;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Unit tests for UC-001 – Personendaten verwalten. */
@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonService personService;

    @Autowired
    private ObjectMapper objectMapper;

    private PersonResponse buildPerson(String mobilenummer) {
        return new PersonResponse(1L, "Hans", "Müller", null, mobilenummer, null);
    }

    private PersonRequest buildRequest(String mobilenummer) {
        return new PersonRequest("Hans", "Müller", null, mobilenummer, null);
    }

    @Test
    @DisplayName("UC-001: GET /api/persons gibt alle Personen zurück")
    void getAll_returnsList() throws Exception {
        when(personService.findAll()).thenReturn(List.of(buildPerson(null)));

        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vorname").value("Hans"))
                .andExpect(jsonPath("$[0].name").value("Müller"))
                // API-001 Stufe 2: null-Felder werden weggelassen
                .andExpect(jsonPath("$[0].mobilenummer").doesNotExist());
    }

    @Test
    @DisplayName("UC-001: POST /api/persons legt eine Person an und gibt sie zurück")
    void create_returnsSavedPerson() throws Exception {
        when(personService.create(any(PersonRequest.class))).thenReturn(buildPerson(null));

        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vorname").value("Hans"));
    }

    @Test
    @DisplayName("UC-001: POST /api/persons mit id im Body wird mit 400 abgelehnt (API-001 Stufe 2, E6)")
    void create_mitId_wirdAbgelehnt() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"vorname\":\"Hans\",\"name\":\"Müller\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unbekanntes Feld: id"));
    }

    @Test
    @DisplayName("UC-001: PUT /api/persons/{id} aktualisiert eine Person")
    void update_returnsUpdatedPerson() throws Exception {
        when(personService.update(eq(1L), any(PersonRequest.class))).thenReturn(buildPerson("+41791234567"));

        mockMvc.perform(put("/api/persons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("+41791234567"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mobilenummer").value("+41791234567"));
    }

    @Test
    @DisplayName("UC-001: DELETE /api/persons/{id} löscht eine Person")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/persons/1"))
                .andExpect(status().isOk());
    }
}
