package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.Einladung;
import ch.quartierfest.backend.einladung.Einladung.BuffetBeitrag;
import ch.quartierfest.backend.event.Event;
import ch.quartierfest.backend.partei.Partei;
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

import static org.mockito.ArgumentMatchers.any;
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

    private Teilnahme buildTeilnahme() {
        Event event = new Event();
        event.setId(1L);
        event.setDatum(LocalDate.of(2025, 7, 5));
        event.setStartzeit(LocalTime.of(15, 0));
        event.setStandort("Buchlenwiese");

        Partei partei = new Partei();
        partei.setId(2L);
        partei.setBezeichnung("Familie Müller");
        partei.setAdresse("Musterstrasse 1");
        partei.setTwintAktiv(false);
        partei.setPersonen(List.of());

        Einladung einladung = new Einladung();
        einladung.setId(3L);
        einladung.setEvent(event);
        einladung.setPartei(partei);
        einladung.setStatus(Einladung.EinladungStatus.ANGEMELDET);
        einladung.setBestaetigungVersendet(false);

        Teilnahme t = new Teilnahme();
        t.setId(4L);
        t.setEinladung(einladung);
        t.setAnzahlPersonenEffektiv(2);
        t.setHilftAufstellen(true);
        return t;
    }

    @Test
    @DisplayName("UC-005: GET /api/teilnahmen gibt alle Teilnahmen zurück")
    void getAll_returnsList() throws Exception {
        when(teilnahmeService.findAll()).thenReturn(List.of(buildTeilnahme()));

        mockMvc.perform(get("/api/teilnahmen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anzahlPersonenEffektiv").value(2))
                .andExpect(jsonPath("$[0].hilftAufstellen").value(true));
    }

    @Test
    @DisplayName("UC-005: POST /api/teilnahmen legt eine Teilnahme an")
    void create_returnsTeilnahme() throws Exception {
        Teilnahme t = buildTeilnahme();
        when(teilnahmeService.save(any(Teilnahme.class))).thenReturn(t);

        mockMvc.perform(post("/api/teilnahmen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.anzahlPersonenEffektiv").value(2));
    }

    @Test
    @DisplayName("UC-005: POST /api/teilnahmen mit mehreren Buffet-Beiträgen")
    void create_withMultipleBeitraege_returnsTeilnahme() throws Exception {
        // UC-005: Mehrere Buffet-Beiträge je Teilnahme möglich (TC-033)
        Teilnahme t = buildTeilnahme();
        t.setBuffetBeitraege(List.of(
                new TeilnahmeBuffetBeitrag(BuffetBeitrag.SALAT, "Grüner Salat"),
                new TeilnahmeBuffetBeitrag(BuffetBeitrag.DESSERT, "Mousse au chocolat")));
        when(teilnahmeService.save(any(Teilnahme.class))).thenReturn(t);

        mockMvc.perform(post("/api/teilnahmen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buffetBeitraege.length()").value(2));
    }

    @Test
    @DisplayName("UC-005: DELETE /api/teilnahmen/{id} löscht eine Teilnahme")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/teilnahmen/4"))
                .andExpect(status().isOk());
    }
}
