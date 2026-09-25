package ch.quartierfest.backend.einladung;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UC-004 – Einladung erstellen und verwalten.
 * Updates (Rückmeldung UC-004, Bestätigung UC-006) laufen über PUT /api/einladungen/{id} (REST-003).
 */
@WebMvcTest(EinladungController.class)
class EinladungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EinladungService einladungService;

    @Autowired
    private ObjectMapper objectMapper;

    private EinladungResponse buildEinladung(boolean bestaetigungVersendet) {
        EventResponse event = new EventResponse(1L, LocalDate.of(2025, 7, 5), LocalTime.of(15, 0),
                "Buchlenwiese", null, null, null);
        ParteiKurz partei = new ParteiKurz(2L, "Familie Müller", "Musterstrasse 1", false, null);
        return new EinladungResponse(3L, event, partei, Einladung.EinladungStatus.OFFEN,
                null, null, null, null, null, bestaetigungVersendet);
    }

    @Test
    @DisplayName("UC-004: GET /api/einladungen gibt alle Einladungen zurück, Partei ohne Personenliste")
    void getAll_returnsList() throws Exception {
        when(einladungService.findAll()).thenReturn(List.of(buildEinladung(false)));

        mockMvc.perform(get("/api/einladungen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OFFEN"))
                .andExpect(jsonPath("$[0].bestaetigungVersendet").value(false))
                .andExpect(jsonPath("$[0].partei.bezeichnung").value("Familie Müller"))
                .andExpect(jsonPath("$[0].partei.personen").doesNotExist());
    }

    @Test
    @DisplayName("UC-004: POST /api/einladungen legt eine Einladung an")
    void create_returnsEinladung() throws Exception {
        when(einladungService.create(any(EinladungRequest.class))).thenReturn(buildEinladung(false));
        EinladungRequest request = new EinladungRequest(1L, 2L, Einladung.EinladungStatus.OFFEN,
                null, null, null, null, null, false);

        mockMvc.perform(post("/api/einladungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("OFFEN"));
    }

    @Test
    @DisplayName("UC-006: PUT /api/einladungen/{id} setzt bestaetigungVersendet (REST-003)")
    void update_setztBestaetigungVersendet() throws Exception {
        when(einladungService.update(eq(3L), any(EinladungUpdateRequest.class))).thenReturn(buildEinladung(true));
        EinladungUpdateRequest request = new EinladungUpdateRequest(Einladung.EinladungStatus.OFFEN,
                null, null, null, null, null, true);

        mockMvc.perform(put("/api/einladungen/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestaetigungVersendet").value(true));
    }

    @Test
    @DisplayName("UC-006: POST /api/einladungen mit id (früherer Upsert) wird mit 400 abgelehnt")
    void create_mitId_wirdAbgelehnt() throws Exception {
        mockMvc.perform(post("/api/einladungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":3,\"eventId\":1,\"parteiId\":2,\"status\":\"OFFEN\",\"bestaetigungVersendet\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unbekanntes Feld: id"));
    }

    @Test
    @DisplayName("UC-004: DELETE /api/einladungen/{id} löscht eine Einladung")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/einladungen/3"))
                .andExpect(status().isOk());
    }
}
