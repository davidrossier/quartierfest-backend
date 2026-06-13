package ch.quartierfest.backend.benutzer;

import ch.quartierfest.backend.partei.Partei;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BenutzerController.class)
class BenutzerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BenutzerService benutzerService;

    @Autowired
    private tools.jackson.databind.ObjectMapper objectMapper;  // Jackson 3.x!

    private Benutzer buildBenutzer() {
        Partei partei = new Partei();
        partei.setId(1L);
        partei.setBezeichnung("Familie Müller");
        partei.setAdresse("Musterstrasse 1");
        partei.setPersonen(List.of());
        Benutzer benutzer = new Benutzer();
        benutzer.setId(5L);
        benutzer.setEmail("mueller@quartier.ch");
        benutzer.setPasswortHash("$2a$hash");
        benutzer.setRolle(Benutzer.Rolle.PARTEI);
        benutzer.setPartei(partei);
        return benutzer;
    }

    @Test
    @DisplayName("UC-015: GET /api/benutzer gibt alle Benutzer ohne Passwortfelder zurück")
    void getAll_returnsListOhnePasswort() throws Exception {
        when(benutzerService.findAll()).thenReturn(List.of(buildBenutzer()));

        mockMvc.perform(get("/api/benutzer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("mueller@quartier.ch"))
                .andExpect(jsonPath("$[0].passwort").doesNotExist())
                .andExpect(jsonPath("$[0].passwortHash").doesNotExist());
    }

    @Test
    @DisplayName("UC-015: POST /api/benutzer legt einen Benutzer an, Antwort ohne Passwortfelder")
    void create_returnsSavedBenutzerOhnePasswort() throws Exception {
        when(benutzerService.save(any(Benutzer.class))).thenReturn(buildBenutzer());

        // Request als Map: das Entity-Feld passwort ist WRITE_ONLY und würde
        // bei der Serialisierung eines Benutzer-Objekts fehlen
        mockMvc.perform(post("/api/benutzer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "mueller@quartier.ch",
                                "passwort", "geheim-1234",
                                "rolle", "PARTEI",
                                "partei", Map.of("id", 1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.passwort").doesNotExist())
                .andExpect(jsonPath("$.passwortHash").doesNotExist());
    }

    @Test
    @DisplayName("UC-015: POST /api/benutzer mit zu kurzem Passwort liefert 400")
    void create_zuKurzesPasswort_liefert400() throws Exception {
        mockMvc.perform(post("/api/benutzer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "mueller@quartier.ch",
                                "passwort", "kurz",
                                "rolle", "PARTEI",
                                "partei", Map.of("id", 1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UC-015: PUT /api/benutzer/{id}/passwort setzt neues Passwort")
    void passwortSetzen_returnsBenutzer() throws Exception {
        when(benutzerService.passwortSetzen(eq(5L), any())).thenReturn(buildBenutzer());

        mockMvc.perform(put("/api/benutzer/5/passwort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("passwort", "neues-geheimnis"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("UC-015: PUT /api/benutzer/{id}/passwort mit zu kurzem Passwort liefert 400")
    void passwortSetzen_zuKurz_liefert400() throws Exception {
        mockMvc.perform(put("/api/benutzer/5/passwort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("passwort", "kurz"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UC-015: DELETE /api/benutzer/{id} löscht den Benutzer")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/benutzer/5"))
                .andExpect(status().isOk());
    }
}
