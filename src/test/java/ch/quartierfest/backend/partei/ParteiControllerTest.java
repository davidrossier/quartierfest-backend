package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.person.PersonResponse;
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

/** Unit tests for UC-002 – Parteien verwalten. */
@WebMvcTest(ParteiController.class)
class ParteiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParteiService parteiService;

    @Autowired
    private ObjectMapper objectMapper;

    private ParteiResponse buildPartei(boolean twintAktiv, String twintMobilenummer) {
        PersonResponse person = new PersonResponse(3L, "Hans", "Müller", null, null, null);
        return new ParteiResponse(1L, "Familie Müller", "Musterstrasse 1", twintAktiv, twintMobilenummer,
                List.of(person));
    }

    private ParteiRequest buildRequest(boolean twintAktiv, String twintMobilenummer) {
        return new ParteiRequest("Familie Müller", "Musterstrasse 1", twintAktiv, twintMobilenummer, List.of(3L));
    }

    @Test
    @DisplayName("UC-002: GET /api/parteien gibt alle Parteien mit ihren Personen zurück")
    void getAll_returnsList() throws Exception {
        when(parteiService.findAll()).thenReturn(List.of(buildPartei(false, null)));

        mockMvc.perform(get("/api/parteien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bezeichnung").value("Familie Müller"))
                .andExpect(jsonPath("$[0].adresse").value("Musterstrasse 1"))
                .andExpect(jsonPath("$[0].personen[0].vorname").value("Hans"));
    }

    @Test
    @DisplayName("UC-002: POST /api/parteien legt eine Partei an und gibt sie zurück")
    void create_returnsSavedPartei() throws Exception {
        when(parteiService.create(any(ParteiRequest.class))).thenReturn(buildPartei(false, null));

        mockMvc.perform(post("/api/parteien")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bezeichnung").value("Familie Müller"));
    }

    @Test
    @DisplayName("UC-002: PUT /api/parteien/{id} aktualisiert eine Partei")
    void update_returnsUpdatedPartei() throws Exception {
        when(parteiService.update(eq(1L), any(ParteiRequest.class))).thenReturn(buildPartei(true, "+41791234567"));

        mockMvc.perform(put("/api/parteien/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(true, "+41791234567"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twintAktiv").value(true))
                .andExpect(jsonPath("$.twintMobilenummer").value("+41791234567"));
    }

    @Test
    @DisplayName("UC-002: DELETE /api/parteien/{id} löscht eine Partei")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/parteien/1"))
                .andExpect(status().isOk());
    }
}
