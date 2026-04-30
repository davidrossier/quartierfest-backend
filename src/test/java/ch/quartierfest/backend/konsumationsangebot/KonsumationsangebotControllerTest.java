package ch.quartierfest.backend.konsumationsangebot;

import ch.quartierfest.backend.event.Event;
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

    private Konsumationsangebot buildAngebot() {
        Event event = new Event();
        event.setId(1L);
        event.setDatum(LocalDate.of(2025, 7, 5));
        event.setStartzeit(LocalTime.of(15, 0));
        event.setStandort("Buchlenwiese");

        Konsumationsangebot k = new Konsumationsangebot();
        k.setId(2L);
        k.setEvent(event);
        k.setBezeichnung("Bier 5dl");
        k.setPreis(new BigDecimal("3.00"));
        return k;
    }

    @Test
    @DisplayName("UC-008: GET /api/konsumationsangebote gibt alle Angebote zurück")
    void getAll_returnsList() throws Exception {
        when(konsumationsangebotService.findAll()).thenReturn(List.of(buildAngebot()));

        mockMvc.perform(get("/api/konsumationsangebote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bezeichnung").value("Bier 5dl"))
                .andExpect(jsonPath("$[0].preis").value(3.00));
    }

    @Test
    @DisplayName("UC-008: POST /api/konsumationsangebote legt ein Angebot an und gibt es zurück")
    void create_returnsSavedAngebot() throws Exception {
        Konsumationsangebot k = buildAngebot();
        when(konsumationsangebotService.save(any(Konsumationsangebot.class))).thenReturn(k);

        mockMvc.perform(post("/api/konsumationsangebote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(k)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.bezeichnung").value("Bier 5dl"));
    }

    @Test
    @DisplayName("UC-008: DELETE /api/konsumationsangebote/{id} löscht ein Angebot")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/konsumationsangebote/2"))
                .andExpect(status().isOk());
    }
}
