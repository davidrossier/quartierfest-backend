package ch.quartierfest.backend.konsumationsangebot;

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

/** Unit tests for UC-008 – Konsumationsangebot verwalten. */
@WebMvcTest(KonsumationsangebotController.class)
class KonsumationsangebotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KonsumationsangebotService konsumationsangebotService;

    @Autowired
    private ObjectMapper objectMapper;

    private KonsumationsangebotResponse buildAngebot(String preis) {
        EventResponse event = new EventResponse(1L, LocalDate.of(2025, 7, 5), LocalTime.of(15, 0),
                "Buchlenwiese", null, null, null);
        return new KonsumationsangebotResponse(2L, event, "Bier 5dl", new BigDecimal(preis));
    }

    private KonsumationsangebotRequest buildRequest(String preis) {
        return new KonsumationsangebotRequest(1L, "Bier 5dl", new BigDecimal(preis));
    }

    @Test
    @DisplayName("UC-008: GET /api/konsumationsangebote gibt alle Angebote zurück")
    void getAll_returnsList() throws Exception {
        when(konsumationsangebotService.findAll()).thenReturn(List.of(buildAngebot("3.00")));

        mockMvc.perform(get("/api/konsumationsangebote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bezeichnung").value("Bier 5dl"))
                .andExpect(jsonPath("$[0].preis").value(3.00))
                .andExpect(jsonPath("$[0].event.id").value(1));
    }

    @Test
    @DisplayName("UC-008: POST /api/konsumationsangebote legt ein Angebot an und gibt es zurück")
    void create_returnsSavedAngebot() throws Exception {
        when(konsumationsangebotService.create(any(KonsumationsangebotRequest.class))).thenReturn(buildAngebot("3.00"));

        mockMvc.perform(post("/api/konsumationsangebote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("3.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.bezeichnung").value("Bier 5dl"));
    }

    @Test
    @DisplayName("UC-008: POST ohne eventId wird mit 400 abgelehnt")
    void create_ohneEventId_wirdAbgelehnt() throws Exception {
        mockMvc.perform(post("/api/konsumationsangebote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bezeichnung\":\"Bier 5dl\",\"preis\":3.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UC-008: PUT /api/konsumationsangebote/{id} aktualisiert ein Angebot (REST-003, erweitert)")
    void update_returnsUpdatedAngebot() throws Exception {
        when(konsumationsangebotService.update(eq(2L), any(KonsumationsangebotRequest.class)))
                .thenReturn(buildAngebot("3.50"));

        mockMvc.perform(put("/api/konsumationsangebote/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("3.50"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preis").value(3.50));
    }

    @Test
    @DisplayName("UC-008: DELETE /api/konsumationsangebote/{id} löscht ein Angebot")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/konsumationsangebote/2"))
                .andExpect(status().isOk());
    }
}
