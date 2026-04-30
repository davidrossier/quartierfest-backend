package ch.quartierfest.backend.mahnung;

import ch.quartierfest.backend.abrechnung.Abrechnung;
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

/** Unit tests for UC-013 – Inkasso sicherstellen (Mahnung). */
@WebMvcTest(MahnungController.class)
class MahnungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MahnungService mahnungService;

    @Autowired
    private ObjectMapper objectMapper;

    private Mahnung buildMahnung() {
        Abrechnung abrechnung = new Abrechnung();
        abrechnung.setId(1L);
        abrechnung.setAnteilAllgemeinkosten(new BigDecimal("40.00"));
        abrechnung.setTotalKonsumation(new BigDecimal("17.00"));
        abrechnung.setTotalBetrag(new BigDecimal("57.00"));
        abrechnung.setZustellungskanal(Abrechnung.Zustellungskanal.EMAIL);

        Mahnung m = new Mahnung();
        m.setId(2L);
        m.setAbrechnung(abrechnung);
        m.setDatum(LocalDate.of(2025, 7, 20));
        m.setBemerkung("Bitte bis Ende Juli bezahlen");
        return m;
    }

    @Test
    @DisplayName("UC-013: GET /api/mahnungen gibt alle Mahnungen zurück")
    void getAll_returnsList() throws Exception {
        when(mahnungService.findAll()).thenReturn(List.of(buildMahnung()));

        mockMvc.perform(get("/api/mahnungen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bemerkung").value("Bitte bis Ende Juli bezahlen"));
    }

    @Test
    @DisplayName("UC-013: POST /api/mahnungen erfasst eine Mahnung und gibt sie zurück")
    void create_returnsSavedMahnung() throws Exception {
        Mahnung m = buildMahnung();
        when(mahnungService.save(any(Mahnung.class))).thenReturn(m);

        mockMvc.perform(post("/api/mahnungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.bemerkung").value("Bitte bis Ende Juli bezahlen"));
    }

    @Test
    @DisplayName("UC-013: DELETE /api/mahnungen/{id} löscht eine Mahnung")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/mahnungen/2"))
                .andExpect(status().isOk());
    }
}
