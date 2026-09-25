package ch.quartierfest.backend.allgemeinausgabe;

import ch.quartierfest.backend.event.EventResponse;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Unit tests for UC-007 – Allgemeinausgabe verwalten. */
@WebMvcTest(AllgemeinausgabeController.class)
class AllgemeinausgabeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AllgemeinausgabeService allgemeinausgabeService;

    @Autowired
    private ObjectMapper objectMapper;

    private AllgemeinausgabeResponse buildAusgabe(String betrag) {
        EventResponse event = new EventResponse(1L, LocalDate.of(2025, 7, 5), LocalTime.of(15, 0),
                "Buchlenwiese", null, null, null);
        return new AllgemeinausgabeResponse(2L, event, "Kühlschrankmiete", "Coop", new BigDecimal(betrag));
    }

    private AllgemeinausgabeRequest buildRequest(String betrag) {
        return new AllgemeinausgabeRequest(1L, "Kühlschrankmiete", "Coop", new BigDecimal(betrag));
    }

    @Test
    @DisplayName("UC-007: GET /api/allgemeinausgaben gibt alle Ausgaben zurück")
    void getAll_returnsList() throws Exception {
        when(allgemeinausgabeService.findAll()).thenReturn(List.of(buildAusgabe("120.00")));

        mockMvc.perform(get("/api/allgemeinausgaben"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].beschreibung").value("Kühlschrankmiete"))
                .andExpect(jsonPath("$[0].betrag").value(120.00));
    }

    @Test
    @DisplayName("UC-007: POST /api/allgemeinausgaben legt eine Ausgabe an und gibt sie zurück")
    void create_returnsSavedAusgabe() throws Exception {
        when(allgemeinausgabeService.create(any(AllgemeinausgabeRequest.class))).thenReturn(buildAusgabe("120.00"));

        mockMvc.perform(post("/api/allgemeinausgaben")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("120.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.beschreibung").value("Kühlschrankmiete"));
    }

    @Test
    @DisplayName("UC-007: PUT /api/allgemeinausgaben/{id} aktualisiert eine Ausgabe (REST-003, erweitert)")
    void update_returnsUpdatedAusgabe() throws Exception {
        when(allgemeinausgabeService.update(eq(2L), any(AllgemeinausgabeRequest.class)))
                .thenReturn(buildAusgabe("150.00"));

        mockMvc.perform(put("/api/allgemeinausgaben/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("150.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betrag").value(150.00));
    }

    @Test
    @DisplayName("UC-007: DELETE /api/allgemeinausgaben/{id} löscht eine Ausgabe")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/allgemeinausgaben/2"))
                .andExpect(status().isOk());
    }
}
