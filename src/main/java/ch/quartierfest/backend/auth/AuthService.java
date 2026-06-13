package ch.quartierfest.backend.auth;

// UC-014: Benutzer anmelden (AUTH-002, Eigenbau-Login)

import ch.quartierfest.backend.benutzer.Benutzer;
import ch.quartierfest.backend.benutzer.BenutzerRepository;
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

@Service
public class AuthService {

    private final BenutzerRepository benutzerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long ttlStunden;

    public AuthService(BenutzerRepository benutzerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       @Value("${auth.jwt.ttl-stunden:12}") long ttlStunden) {
        this.benutzerRepository = benutzerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.ttlStunden = ttlStunden;
    }

    public String login(String email, String passwort) {
        // Bewusst dieselbe Meldung für unbekannte E-Mail und falsches Passwort (UC-014 E1)
        Benutzer benutzer = benutzerRepository.findByEmail(email)
                .filter(b -> passwordEncoder.matches(passwort, b.getPasswortHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "E-Mail-Adresse oder Passwort falsch."));

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
