package ch.quartierfest.backend.zahlung;

import ch.quartierfest.backend.abrechnung.Abrechnung;
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

/** Unit tests for UC-013 – Inkasso sicherstellen (Zahlung). */
@WebMvcTest(ZahlungController.class)
class ZahlungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZahlungService zahlungService;

    @Autowired
    private ObjectMapper objectMapper;

    private Zahlung buildZahlung() {
        Abrechnung abrechnung = new Abrechnung();
        abrechnung.setId(1L);
        abrechnung.setAnteilAllgemeinkosten(new BigDecimal("40.00"));
        abrechnung.setTotalKonsumation(new BigDecimal("17.00"));
        abrechnung.setTotalBetrag(new BigDecimal("57.00"));
        abrechnung.setZustellungskanal(Abrechnung.Zustellungskanal.TWINT);

        Zahlung z = new Zahlung();
        z.setId(2L);
        z.setAbrechnung(abrechnung);
        z.setZahlungskanal(Zahlung.Zahlungskanal.TWINT);
        z.setDatum(LocalDate.of(2025, 7, 15));
        z.setBetrag(new BigDecimal("57.00"));
        return z;
    }

    @Test
    @DisplayName("UC-013: GET /api/zahlungen gibt alle Zahlungen zurück")
    void getAll_returnsList() throws Exception {
        when(zahlungService.findAll()).thenReturn(List.of(buildZahlung()));

        mockMvc.perform(get("/api/zahlungen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zahlungskanal").value("TWINT"))
                .andExpect(jsonPath("$[0].betrag").value(57.00));
    }

    @Test
    @DisplayName("UC-013: POST /api/zahlungen erfasst eine Zahlung und gibt sie zurück")
    void create_returnsSavedZahlung() throws Exception {
        Zahlung z = buildZahlung();
        when(zahlungService.save(any(Zahlung.class))).thenReturn(z);

        mockMvc.perform(post("/api/zahlungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(z)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.zahlungskanal").value("TWINT"));
    }

    @Test
    @DisplayName("UC-013: DELETE /api/zahlungen/{id} löscht eine Zahlung")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/zahlungen/2"))
                .andExpect(status().isOk());
    }
}
