package ch.quartierfest.backend.abrechnung;

import ch.quartierfest.backend.einladung.Einladung;
import ch.quartierfest.backend.einladung.EinladungKurz;
import ch.quartierfest.backend.event.EventResponse;
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

/** Unit tests for UC-011 – Abrechnung erstellen, UC-012 – Abrechnung zustellen. */
@WebMvcTest(AbrechnungController.class)
class AbrechnungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AbrechnungService abrechnungService;

    @Autowired
    private ObjectMapper objectMapper;

    private AbrechnungResponse buildAbrechnung(LocalDate zustellungsDatum) {
        EventResponse event = new EventResponse(1L, LocalDate.of(2025, 7, 5), LocalTime.of(15, 0),
                "Buchlenwiese", null, null, null);
        ParteiKurz partei = new ParteiKurz(2L, "Familie Müller", "Musterstrasse 1", true, "+41791234567");
        TeilnahmeKurz teilnahme = new TeilnahmeKurz(4L,
                new EinladungKurz(3L, Einladung.EinladungStatus.ANGEMELDET, 2, event, partei));
        return new AbrechnungResponse(5L, teilnahme, new BigDecimal("40.00"), new BigDecimal("17.00"),
                new BigDecimal("57.00"), Abrechnung.Zustellungskanal.EMAIL, zustellungsDatum);
    }

    @Test
    @DisplayName("UC-011: GET /api/abrechnungen gibt alle Abrechnungen zurück")
    void getAll_returnsList() throws Exception {
        when(abrechnungService.findAll()).thenReturn(List.of(buildAbrechnung(null)));

        mockMvc.perform(get("/api/abrechnungen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zustellungskanal").value("EMAIL"))
                .andExpect(jsonPath("$[0].totalBetrag").value(57.00))
                .andExpect(jsonPath("$[0].teilnahme.einladung.partei.twintMobilenummer").value("+41791234567"));
    }

    @Test
    @DisplayName("UC-011: POST /api/abrechnungen erstellt eine Abrechnung und gibt sie zurück")
    void create_returnsSavedAbrechnung() throws Exception {
        when(abrechnungService.create(any(AbrechnungRequest.class))).thenReturn(buildAbrechnung(null));
        AbrechnungRequest request = new AbrechnungRequest(4L, new BigDecimal("40.00"), new BigDecimal("17.00"),
                new BigDecimal("57.00"), Abrechnung.Zustellungskanal.EMAIL, null);

        mockMvc.perform(post("/api/abrechnungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.zustellungskanal").value("EMAIL"));
    }

    @Test
    @DisplayName("UC-012: PUT /api/abrechnungen/{id} setzt zustellungsDatum (REST-003)")
    void update_setztZustellungsDatum() throws Exception {
        when(abrechnungService.update(eq(5L), any(AbrechnungUpdateRequest.class)))
                .thenReturn(buildAbrechnung(LocalDate.of(2025, 7, 10)));
        AbrechnungUpdateRequest request = new AbrechnungUpdateRequest(new BigDecimal("40.00"),
                new BigDecimal("17.00"), new BigDecimal("57.00"), Abrechnung.Zustellungskanal.EMAIL,
                LocalDate.of(2025, 7, 10));

        mockMvc.perform(put("/api/abrechnungen/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zustellungsDatum").value("2025-07-10"));
    }

    @Test
    @DisplayName("UC-011: DELETE /api/abrechnungen/{id} löscht eine Abrechnung")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/abrechnungen/5"))
                .andExpect(status().isOk());
    }
}
