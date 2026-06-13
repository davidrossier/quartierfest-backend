package ch.quartierfest.backend.benutzer;

// UC-015: Benutzer verwalten (AUTH-002)

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BenutzerService {

    private final BenutzerRepository benutzerRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Benutzer> findAll() {
        return benutzerRepository.findAll();
    }

    public Benutzer save(Benutzer benutzer) {
        if (benutzerRepository.existsByEmail(benutzer.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Für diese E-Mail-Adresse existiert bereits ein Account.");
        }
        if (benutzer.getRolle() == Benutzer.Rolle.PARTEI && benutzer.getPartei() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Für die Rolle PARTEI muss eine Partei zugeordnet werden.");
        }
        benutzer.setPasswortHash(passwordEncoder.encode(benutzer.getPasswort()));
        // passwort erst nach dem save() leeren: Hibernate validiert beim Persistieren
        // auch @Transient-Felder (@NotBlank)
        Benutzer gespeichert = benutzerRepository.save(benutzer);
        gespeichert.setPasswort(null);
        return gespeichert;
    }

    public void delete(Long id) {
        Benutzer benutzer = benutzerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden."));
        if (benutzer.getRolle() == Benutzer.Rolle.ORGANISATOR
                && benutzerRepository.countByRolle(Benutzer.Rolle.ORGANISATOR) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Der letzte Organisator-Account kann nicht gelöscht werden.");
        }
        benutzerRepository.deleteById(id);
    }

    public Benutzer passwortSetzen(Long id, String neuesPasswort) {
        Benutzer benutzer = benutzerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden."));
        benutzer.setPasswort(neuesPasswort);
        benutzer.setPasswortHash(passwordEncoder.encode(neuesPasswort));
        Benutzer gespeichert = benutzerRepository.save(benutzer);
        gespeichert.setPasswort(null);
        return gespeichert;
    }
}
