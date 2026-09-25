package ch.quartierfest.backend.mahnung;

import ch.quartierfest.backend.abrechnung.AbrechnungKurz;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Unit tests for UC-013 – Inkasso sicherstellen (Mahnungen). */
@WebMvcTest(MahnungController.class)
class MahnungControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MahnungService mahnungService;

    @Autowired
    private ObjectMapper objectMapper;

    private MahnungResponse buildMahnung() {
        return new MahnungResponse(2L, new AbrechnungKurz(5L), LocalDate.of(2025, 7, 20),
                "Bitte bis Ende Juli bezahlen");
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
        when(mahnungService.create(any(MahnungRequest.class))).thenReturn(buildMahnung());

        mockMvc.perform(post("/api/mahnungen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MahnungRequest(5L, LocalDate.of(2025, 7, 20), "Bitte bis Ende Juli bezahlen"))))
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
