package ch.quartierfest.backend.allgemeinausgabe;

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

/** Unit tests for UC-007 – Allgemeinausgaben verwalten. */
@WebMvcTest(AllgemeinausgabeController.class)
class AllgemeinausgabeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AllgemeinausgabeService allgemeinausgabeService;

    @Autowired
    private ObjectMapper objectMapper;

    private Allgemeinausgabe buildAusgabe() {
        Event event = new Event();
        event.setId(1L);
        event.setDatum(LocalDate.of(2025, 7, 5));
        event.setStartzeit(LocalTime.of(15, 0));
        event.setStandort("Buchlenwiese");

        Allgemeinausgabe a = new Allgemeinausgabe();
        a.setId(2L);
        a.setEvent(event);
        a.setBeschreibung("Kühlschrankmiete");
        a.setHerkunft("Coop");
        a.setBetrag(new BigDecimal("120.00"));
        return a;
    }

    @Test
    @DisplayName("UC-007: GET /api/allgemeinausgaben gibt alle Ausgaben zurück")
    void getAll_returnsList() throws Exception {
        when(allgemeinausgabeService.findAll()).thenReturn(List.of(buildAusgabe()));

        mockMvc.perform(get("/api/allgemeinausgaben"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].beschreibung").value("Kühlschrankmiete"))
                .andExpect(jsonPath("$[0].betrag").value(120.00));
    }

    @Test
    @DisplayName("UC-007: POST /api/allgemeinausgaben legt eine Ausgabe an und gibt sie zurück")
    void create_returnsSavedAusgabe() throws Exception {
        Allgemeinausgabe a = buildAusgabe();
        when(allgemeinausgabeService.save(any(Allgemeinausgabe.class))).thenReturn(a);

        mockMvc.perform(post("/api/allgemeinausgaben")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.beschreibung").value("Kühlschrankmiete"));
    }

    @Test
    @DisplayName("UC-007: DELETE /api/allgemeinausgaben/{id} löscht eine Ausgabe")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/allgemeinausgaben/2"))
                .andExpect(status().isOk());
    }
}
