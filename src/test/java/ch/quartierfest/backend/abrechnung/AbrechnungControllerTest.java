package ch.quartierfest.backend.abrechnung;

import ch.quartierfest.backend.einladung.Einladung;
import ch.quartierfest.backend.event.Event;
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

/**
 * Unit tests for UC-011 – Abrechnung erstellen, UC-012 – Abrechnung zustellen.
 * POST /api/abrechnungen agiert auch als Upsert (UC-012 zustellungsDatum).
 */
@WebMvcTest(AbrechnungController.class)
class AbrechnungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AbrechnungService abrechnungService;

    @Autowired
    private ObjectMapper objectMapper;

    private Abrechnung buildAbrechnung() {
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

        Abrechnung a = new Abrechnung();
        a.setId(5L);
        a.setTeilnahme(teilnahme);
        a.setAnteilAllgemeinkosten(new BigDecimal("40.00"));
        a.setTotalKonsumation(new BigDecimal("17.00"));
        a.setTotalBetrag(new BigDecimal("57.00"));
        a.setZustellungskanal(Abrechnung.Zustellungskanal.EMAIL);
        return a;
    }

    @Test
    @DisplayName("UC-011: GET /api/abrechnungen gibt alle Abrechnungen zurück")
    void getAll_returnsList() throws Exception {
        when(abrechnungService.findAll()).thenReturn(List.of(buildAbrechnung()));

        mockMvc.perform(get("/api/abrechnungen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zustellungskanal").value("EMAIL"))
                .andExpect(jsonPath("$[0].totalBetrag").value(57.00));
    }

    @Test
    @DisplayName("UC-011: POST /api/abrechnungen erstellt eine Abrechnung und gibt sie zurück")
    void create_returnsSavedAbrechnung() throws Exception {
        Abrechnung a = buildAbrechnung();
        when(abrechnungService.save(any(Abrechnung.class))).thenReturn(a);

        mockMvc.perform(post("/api/abrechnungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.zustellungskanal").value("EMAIL"));
    }

    @Test
    @DisplayName("UC-012: POST /api/abrechnungen mit id setzt zustellungsDatum via Upsert")
    void upsert_setsZustellungsDatum() throws Exception {
        // UC-012: POST mit id im Body agiert als Upsert (JPA save() mit vorhandener ID)
        Abrechnung a = buildAbrechnung();
        a.setZustellungsDatum(LocalDate.of(2025, 7, 10));
        when(abrechnungService.save(any(Abrechnung.class))).thenReturn(a);

        mockMvc.perform(post("/api/abrechnungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zustellungskanal").value("EMAIL"));
    }

    @Test
    @DisplayName("UC-011: DELETE /api/abrechnungen/{id} löscht eine Abrechnung")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/abrechnungen/5"))
                .andExpect(status().isOk());
    }
}
