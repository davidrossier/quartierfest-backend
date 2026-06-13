package ch.quartierfest.backend.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private tools.jackson.databind.ObjectMapper objectMapper;  // Jackson 3.x!

    @Test
    @DisplayName("UC-014: POST /api/auth/login liefert Token bei korrekten Credentials")
    void login_korrekt_liefertToken() throws Exception {
        when(authService.login("mueller@quartier.ch", "geheim-1234")).thenReturn("ey.test.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "mueller@quartier.ch", "passwort", "geheim-1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("ey.test.token"));
    }

    @Test
    @DisplayName("UC-014: POST /api/auth/login mit falschen Credentials liefert 401")
    void login_falsch_liefert401() throws Exception {
        when(authService.login("mueller@quartier.ch", "falsch"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "E-Mail-Adresse oder Passwort falsch."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "mueller@quartier.ch", "passwort", "falsch"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("UC-014: POST /api/auth/login ohne E-Mail liefert 400")
    void login_ohneEmail_liefert400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("passwort", "geheim-1234"))))
                .andExpect(status().isBadRequest());
    }
}
