package ch.quartierfest.backend.auth;

import ch.quartierfest.backend.benutzer.Benutzer;
import ch.quartierfest.backend.benutzer.BenutzerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private BenutzerRepository benutzerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(benutzerRepository, passwordEncoder, jwtEncoder, 12);
    }

    private Benutzer buildBenutzer() {
        Benutzer benutzer = new Benutzer();
        benutzer.setId(42L);
        benutzer.setEmail("mueller@quartier.ch");
        benutzer.setPasswortHash("$2a$hash");
        benutzer.setRolle(Benutzer.Rolle.PARTEI);
        return benutzer;
    }

    private Jwt buildJwt() {
        return Jwt.withTokenValue("ey.test.token")
                .header("alg", "HS256")
                .claims(c -> c.putAll(Map.of("sub", "42")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    @DisplayName("UC-014: login() mit korrekten Credentials liefert Token mit sub und rolle")
    void login_korrekt_liefertToken() {
        when(benutzerRepository.findByEmail("mueller@quartier.ch")).thenReturn(Optional.of(buildBenutzer()));
        when(passwordEncoder.matches("geheim-1234", "$2a$hash")).thenReturn(true);
        when(jwtEncoder.encode(any())).thenReturn(buildJwt());

        String token = authService.login("mueller@quartier.ch", "geheim-1234");

        assertThat(token).isEqualTo("ey.test.token");
        ArgumentCaptor<JwtEncoderParameters> params = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(params.capture());
        assertThat(params.getValue().getClaims().getSubject()).isEqualTo("42");
        assertThat(params.getValue().getClaims().getClaim("rolle").toString()).isEqualTo("PARTEI");
    }

    @Test
    @DisplayName("UC-014: login() mit falschem Passwort wirft 401")
    void login_falschesPasswort_wirft401() {
        when(benutzerRepository.findByEmail("mueller@quartier.ch")).thenReturn(Optional.of(buildBenutzer()));
        when(passwordEncoder.matches("falsch", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("mueller@quartier.ch", "falsch"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("UC-014: login() mit unbekannter E-Mail wirft 401 (gleiche Meldung)")
    void login_unbekannteEmail_wirft401() {
        when(benutzerRepository.findByEmail("unbekannt@quartier.ch")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unbekannt@quartier.ch", "egal"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
