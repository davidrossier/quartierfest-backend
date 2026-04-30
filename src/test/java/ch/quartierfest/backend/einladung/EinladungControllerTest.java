package ch.quartierfest.backend.einladung;

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

/**
 * Unit tests for UC-004 – Einladung erstellen und verwalten.
 * POST /api/einladungen agiert auch als Upsert (UC-006 bestaetigungVersendet).
 */
@WebMvcTest(EinladungController.class)
class EinladungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EinladungService einladungService;

    @Autowired
    private ObjectMapper objectMapper;

    private Einladung buildEinladung() {
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

        Einladung e = new Einladung();
        e.setId(3L);
        e.setEvent(event);
        e.setPartei(partei);
        e.setStatus(Einladung.EinladungStatus.OFFEN);
        e.setBestaetigungVersendet(false);
        return e;
    }

    @Test
    @DisplayName("UC-004: GET /api/einladungen gibt alle Einladungen zurück")
    void getAll_returnsList() throws Exception {
        when(einladungService.findAll()).thenReturn(List.of(buildEinladung()));

        mockMvc.perform(get("/api/einladungen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OFFEN"))
                .andExpect(jsonPath("$[0].bestaetigungVersendet").value(false));
    }

    @Test
    @DisplayName("UC-004: POST /api/einladungen legt eine Einladung an (Status OFFEN)")
    void create_returnsEinladung() throws Exception {
        Einladung e = buildEinladung();
        when(einladungService.save(any(Einladung.class))).thenReturn(e);

        mockMvc.perform(post("/api/einladungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(e)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("OFFEN"));
    }

    @Test
    @DisplayName("UC-006: POST /api/einladungen mit id setzt bestaetigungVersendet via Upsert")
    void upsert_setsbestaetigungVersendet() throws Exception {
        // UC-006: POST mit id im Body agiert als Upsert (JPA save() mit vorhandener ID)
        Einladung e = buildEinladung();
        e.setBestaetigungVersendet(true);
        when(einladungService.save(any(Einladung.class))).thenReturn(e);

        mockMvc.perform(post("/api/einladungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(e)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestaetigungVersendet").value(true));
    }

    @Test
    @DisplayName("UC-004: DELETE /api/einladungen/{id} löscht eine Einladung")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/einladungen/3"))
                .andExpect(status().isOk());
    }
}
