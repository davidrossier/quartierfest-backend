package ch.quartierfest.backend.auth;

// UC-015: Bootstrap des ersten ORGANISATOR-Accounts (AUTH-002) — löst das
// Henne-Ei-Problem: ohne Account kein Login, ohne Login keine Benutzerverwaltung.

import ch.quartierfest.backend.benutzer.Benutzer;
import ch.quartierfest.backend.benutzer.BenutzerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(BootstrapConfig.class);

    @Bean
    public ApplicationRunner bootstrapOrganisator(BenutzerRepository benutzerRepository,
                                                  PasswordEncoder passwordEncoder,
                                                  @Value("${auth.bootstrap.email}") String email,
                                                  @Value("${auth.bootstrap.password}") String password) {
        return args -> {
            if (benutzerRepository.countByRolle(Benutzer.Rolle.ORGANISATOR) == 0) {
                Benutzer admin = new Benutzer();
                admin.setEmail(email);
                // passwort muss beim Persistieren gesetzt sein (@Transient-Validierung); min. 10 Zeichen
                admin.setPasswort(password);
                admin.setPasswortHash(passwordEncoder.encode(password));
                admin.setRolle(Benutzer.Rolle.ORGANISATOR);
                benutzerRepository.save(admin);
                log.info("Bootstrap-ORGANISATOR '{}' angelegt (kein ORGANISATOR vorhanden).", email);
            }
        };
    }
}
