package ch.quartierfest.backend.auth;

// UC-014: Benutzer anmelden (AUTH-002, Eigenbau-Login; SEC-002 Brute-Force-Drosselung)

import ch.quartierfest.backend.benutzer.Benutzer;
import ch.quartierfest.backend.benutzer.BenutzerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final BenutzerRepository benutzerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final LoginDrosselung loginDrosselung;
    private final long ttlStunden;

    public AuthService(BenutzerRepository benutzerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       LoginDrosselung loginDrosselung,
                       @Value("${auth.jwt.ttl-stunden:12}") long ttlStunden) {
        this.benutzerRepository = benutzerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.loginDrosselung = loginDrosselung;
        this.ttlStunden = ttlStunden;
    }

    public String login(String email, String passwort, String clientIp) {
        // SEC-002: gesperrte E-Mail/IP wird abgewiesen, bevor Credentials geprüft werden
        if (loginDrosselung.istGesperrt(email, clientIp)) {
            log.warn("Login für '{}' von {} abgewiesen: zu viele Fehlversuche.", email, clientIp);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Zu viele Fehlversuche. Bitte später erneut versuchen.");
        }

        // Bewusst dieselbe Meldung für unbekannte E-Mail und falsches Passwort (UC-014 E1)
        Optional<Benutzer> gefunden = benutzerRepository.findByEmail(email)
                .filter(b -> passwordEncoder.matches(passwort, b.getPasswortHash()));
        if (gefunden.isEmpty()) {
            loginDrosselung.fehlversuch(email, clientIp);
            log.warn("Fehlgeschlagener Login für '{}' von {}.", email, clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-Mail-Adresse oder Passwort falsch.");
        }
        Benutzer benutzer = gefunden.get();
        loginDrosselung.erfolg(email, clientIp);

        Instant jetzt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(benutzer.getId()))
                .issuedAt(jetzt)
                .expiresAt(jetzt.plus(ttlStunden, ChronoUnit.HOURS))
                .claim("email", benutzer.getEmail())
                .claim("rolle", benutzer.getRolle().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
