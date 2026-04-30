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

    private Person buildPerson() {
        Person p = new Person();
        p.setId(1L);
        p.setVorname("Hans");
        p.setName("Müller");
        return p;
    }

    @Test
    @DisplayName("UC-001: GET /api/persons gibt alle Personen zurück")
    void getAll_returnsList() throws Exception {
        when(personService.findAll()).thenReturn(List.of(buildPerson()));

        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vorname").value("Hans"))
                .andExpect(jsonPath("$[0].name").value("Müller"));
    }

    @Test
    @DisplayName("UC-001: POST /api/persons legt eine Person an und gibt sie zurück")
    void create_returnsSavedPerson() throws Exception {
        Person p = buildPerson();
        when(personService.save(any(Person.class))).thenReturn(p);

        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vorname").value("Hans"));
    }

    @Test
    @DisplayName("UC-001: PUT /api/persons/{id} aktualisiert eine Person")
    void update_returnsUpdatedPerson() throws Exception {
        Person p = buildPerson();
        p.setMobilenummer("+41791234567");
        when(personService.save(any(Person.class))).thenReturn(p);

        mockMvc.perform(put("/api/persons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
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
