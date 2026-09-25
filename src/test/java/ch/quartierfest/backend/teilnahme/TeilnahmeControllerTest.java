package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.Einladung;
import ch.quartierfest.backend.einladung.Einladung.BuffetBeitrag;
import ch.quartierfest.backend.einladung.EinladungKurz;
import ch.quartierfest.backend.event.EventResponse;
import ch.quartierfest.backend.partei.ParteiKurz;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Unit tests for UC-005 – Teilnahmen verwalten. */
@WebMvcTest(TeilnahmeController.class)
class TeilnahmeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeilnahmeService teilnahmeService;

    @Autowired
    private ObjectMapper objectMapper;

    private TeilnahmeResponse buildTeilnahme(int anzahl, List<TeilnahmeBuffetBeitrag> beitraege) {
        EventResponse event = new EventResponse(1L, LocalDate.of(2025, 7, 5), LocalTime.of(15, 0),
                "Buchlenwiese", null, null, null);
        ParteiKurz partei = new ParteiKurz(2L, "Familie Müller", "Musterstrasse 1", false, null);
        EinladungKurz einladung = new EinladungKurz(3L, Einladung.EinladungStatus.ANGEMELDET, 2, event, partei);
        return new TeilnahmeResponse(4L, einladung, anzahl, true, false, beitraege);
    }

    @Test
    @DisplayName("UC-005: GET /api/teilnahmen gibt alle Teilnahmen zurück")
    void getAll_returnsList() throws Exception {
        when(teilnahmeService.findAll()).thenReturn(List.of(buildTeilnahme(2, List.of())));

        mockMvc.perform(get("/api/teilnahmen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anzahlPersonenEffektiv").value(2))
                .andExpect(jsonPath("$[0].hilftAufstellen").value(true))
                .andExpect(jsonPath("$[0].einladung.event.id").value(1))
                .andExpect(jsonPath("$[0].einladung.partei.bezeichnung").value("Familie Müller"));
    }

    @Test
    @DisplayName("UC-005: POST /api/teilnahmen legt eine Teilnahme an")
    void create_returnsTeilnahme() throws Exception {
        when(teilnahmeService.create(any(TeilnahmeRequest.class))).thenReturn(buildTeilnahme(2, List.of()));

        mockMvc.perform(post("/api/teilnahmen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeilnahmeRequest(3L, 2, true, false, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.anzahlPersonenEffektiv").value(2));
    }

    @Test
    @DisplayName("UC-005: POST /api/teilnahmen mit id wird abgelehnt (REST-001)")
    void create_mitId_wirdAbgelehnt() throws Exception {
        mockMvc.perform(post("/api/teilnahmen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", 4, "einladungId", 3, "anzahlPersonenEffektiv", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unbekanntes Feld: id"));
    }

    @Test
    @DisplayName("UC-005: POST /api/teilnahmen mit mehreren Buffet-Beiträgen")
    void create_withMultipleBeitraege_returnsTeilnahme() throws Exception {
        // UC-005: Mehrere Buffet-Beiträge je Teilnahme möglich (TC-033)
        List<TeilnahmeBuffetBeitrag> beitraege = List.of(
                new TeilnahmeBuffetBeitrag(BuffetBeitrag.SALAT, "Grüner Salat"),
                new TeilnahmeBuffetBeitrag(BuffetBeitrag.DESSERT, "Mousse au chocolat"));
        when(teilnahmeService.create(any(TeilnahmeRequest.class))).thenReturn(buildTeilnahme(2, beitraege));

        mockMvc.perform(post("/api/teilnahmen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeilnahmeRequest(3L, 2, true, false, beitraege))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buffetBeitraege.length()").value(2));
    }

    @Test
    @DisplayName("UC-005: Buffet-Beitrag ohne art wird mit 400 abgelehnt (API-001 Stufe 2)")
    void create_beitragOhneArt_wirdAbgelehnt() throws Exception {
        mockMvc.perform(post("/api/teilnahmen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "einladungId", 3,
                                "buffetBeitraege", List.of(Map.of("beschreibung", "ohne Art"))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validierung fehlgeschlagen: buffetBeitraege[0].art: must not be null"));
    }

    @Test
    @DisplayName("UC-005: DELETE /api/teilnahmen/{id} löscht eine Teilnahme")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/teilnahmen/4"))
                .andExpect(status().isOk());
    }

    // GET /api/teilnahmen/meine wird nicht im MVC-Slice getestet: der
    // @AuthenticationPrincipal-Resolver ist dort nicht registriert. Abdeckung
    // end-to-end mit echtem JWT in TeilnahmeBestaetigenIT (TC-036, inkl. 401 ohne Token).

    @Test
    @DisplayName("UC-016: PUT /api/teilnahmen/{id} aktualisiert die Whitelist-Felder")
    void update_returnsAktualisierteTeilnahme() throws Exception {
        when(teilnahmeService.update(eq(4L), any(TeilnahmeUpdateRequest.class))).thenReturn(buildTeilnahme(4, List.of()));

        mockMvc.perform(put("/api/teilnahmen/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "anzahlPersonenEffektiv", 4,
                                "hilftAufstellen", true,
                                "hilftAufraumen", false,
                                "buffetBeitraege", List.of(
                                        Map.of("art", "SALAT", "beschreibung", "Rüebli-Salat"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anzahlPersonenEffektiv").value(4));
    }
}
