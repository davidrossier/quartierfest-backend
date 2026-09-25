package ch.quartierfest.backend.konsumation;

import ch.quartierfest.backend.einladung.Einladung;
import ch.quartierfest.backend.einladung.EinladungKurz;
import ch.quartierfest.backend.event.EventResponse;
import ch.quartierfest.backend.konsumationsangebot.KonsumationsangebotKurz;
import ch.quartierfest.backend.partei.ParteiKurz;
import ch.quartierfest.backend.teilnahme.TeilnahmeKurz;
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

/** Unit tests for UC-010 – Konsumation übernehmen. */
@WebMvcTest(KonsumationController.class)
class KonsumationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KonsumationService konsumationService;

    @Autowired
    private ObjectMapper objectMapper;

    private KonsumationResponse buildKonsumation(int anzahl) {
        EventResponse event = new EventResponse(1L, LocalDate.of(2025, 7, 5), LocalTime.of(15, 0),
                "Buchlenwiese", null, null, null);
        ParteiKurz partei = new ParteiKurz(2L, "Familie Müller", "Musterstrasse 1", false, null);
        TeilnahmeKurz teilnahme = new TeilnahmeKurz(4L,
                new EinladungKurz(3L, Einladung.EinladungStatus.ANGEMELDET, 2, event, partei));
        KonsumationsangebotKurz angebot = new KonsumationsangebotKurz(5L, "Bier 5dl", new BigDecimal("3.00"));
        return new KonsumationResponse(6L, teilnahme, angebot, anzahl);
    }

    @Test
    @DisplayName("UC-010: GET /api/konsumationen gibt alle Konsumationen zurück")
    void getAll_returnsList() throws Exception {
        when(konsumationService.findAll()).thenReturn(List.of(buildKonsumation(3)));

        mockMvc.perform(get("/api/konsumationen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anzahl").value(3))
                .andExpect(jsonPath("$[0].konsumationsangebot.preis").value(3.00))
                .andExpect(jsonPath("$[0].teilnahme.einladung.event.id").value(1))
                .andExpect(jsonPath("$[0].teilnahme.buffetBeitraege").doesNotExist());
    }

    @Test
    @DisplayName("UC-010: POST /api/konsumationen erfasst eine Konsumation und gibt sie zurück")
    void create_returnsSavedKonsumation() throws Exception {
        when(konsumationService.create(any(KonsumationRequest.class))).thenReturn(buildKonsumation(3));

        mockMvc.perform(post("/api/konsumationen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KonsumationRequest(4L, 5L, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.anzahl").value(3));
    }

    @Test
    @DisplayName("UC-010: PUT /api/konsumationen/{id} ändert die Anzahl (REST-003, erweitert)")
    void update_aendertAnzahl() throws Exception {
        when(konsumationService.update(eq(6L), any(KonsumationUpdateRequest.class))).thenReturn(buildKonsumation(5));

        mockMvc.perform(put("/api/konsumationen/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KonsumationUpdateRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anzahl").value(5));
    }

    @Test
    @DisplayName("UC-010: DELETE /api/konsumationen/{id} löscht eine Konsumation")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/konsumationen/6"))
                .andExpect(status().isOk());
    }
}
