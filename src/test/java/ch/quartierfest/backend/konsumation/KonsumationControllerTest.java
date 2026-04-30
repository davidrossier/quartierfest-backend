package ch.quartierfest.backend.konsumation;

import ch.quartierfest.backend.einladung.Einladung;
import ch.quartierfest.backend.event.Event;
import ch.quartierfest.backend.konsumationsangebot.Konsumationsangebot;
import ch.quartierfest.backend.partei.Partei;
import ch.quartierfest.backend.teilnahme.Teilnahme;
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

/** Unit tests for UC-010 – Konsumation übernehmen. */
@WebMvcTest(KonsumationController.class)
class KonsumationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KonsumationService konsumationService;

    @Autowired
    private ObjectMapper objectMapper;

    private Konsumation buildKonsumation() {
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

        Teilnahme teilnahme = new Teilnahme();
        teilnahme.setId(4L);
        teilnahme.setEinladung(einladung);
        teilnahme.setAnzahlPersonenEffektiv(2);

        Konsumationsangebot angebot = new Konsumationsangebot();
        angebot.setId(5L);
        angebot.setEvent(event);
        angebot.setBezeichnung("Bier 5dl");
        angebot.setPreis(new BigDecimal("3.00"));

        Konsumation k = new Konsumation();
        k.setId(6L);
        k.setTeilnahme(teilnahme);
        k.setKonsumationsangebot(angebot);
        k.setAnzahl(3);
        return k;
    }

    @Test
    @DisplayName("UC-010: GET /api/konsumationen gibt alle Konsumationen zurück")
    void getAll_returnsList() throws Exception {
        when(konsumationService.findAll()).thenReturn(List.of(buildKonsumation()));

        mockMvc.perform(get("/api/konsumationen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anzahl").value(3));
    }

    @Test
    @DisplayName("UC-010: POST /api/konsumationen erfasst eine Konsumation und gibt sie zurück")
    void create_returnsSavedKonsumation() throws Exception {
        Konsumation k = buildKonsumation();
        when(konsumationService.save(any(Konsumation.class))).thenReturn(k);

        mockMvc.perform(post("/api/konsumationen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(k)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.anzahl").value(3));
    }

    @Test
    @DisplayName("UC-010: DELETE /api/konsumationen/{id} löscht eine Konsumation")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/konsumationen/6"))
                .andExpect(status().isOk());
    }
}
