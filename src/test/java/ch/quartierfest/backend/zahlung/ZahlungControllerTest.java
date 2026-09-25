package ch.quartierfest.backend.zahlung;

import ch.quartierfest.backend.abrechnung.AbrechnungKurz;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Unit tests for UC-013 – Inkasso sicherstellen (Zahlungen). */
@WebMvcTest(ZahlungController.class)
class ZahlungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZahlungService zahlungService;

    @Autowired
    private ObjectMapper objectMapper;

    private ZahlungResponse buildZahlung() {
        return new ZahlungResponse(2L, new AbrechnungKurz(5L), Zahlung.Zahlungskanal.TWINT,
                LocalDate.of(2025, 7, 15), new BigDecimal("57.00"));
    }

    @Test
    @DisplayName("UC-013: GET /api/zahlungen gibt alle Zahlungen zurück")
    void getAll_returnsList() throws Exception {
        when(zahlungService.findAll()).thenReturn(List.of(buildZahlung()));

        mockMvc.perform(get("/api/zahlungen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zahlungskanal").value("TWINT"))
                .andExpect(jsonPath("$[0].betrag").value(57.00))
                .andExpect(jsonPath("$[0].abrechnung.id").value(5))
                .andExpect(jsonPath("$[0].abrechnung.teilnahme").doesNotExist());
    }

    @Test
    @DisplayName("UC-013: POST /api/zahlungen erfasst eine Zahlung und gibt sie zurück")
    void create_returnsSavedZahlung() throws Exception {
        when(zahlungService.create(any(ZahlungRequest.class))).thenReturn(buildZahlung());
        ZahlungRequest request = new ZahlungRequest(5L, Zahlung.Zahlungskanal.TWINT, LocalDate.of(2025, 7, 15),
                new BigDecimal("57.00"));

        mockMvc.perform(post("/api/zahlungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
