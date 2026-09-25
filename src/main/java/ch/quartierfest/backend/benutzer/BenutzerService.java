package ch.quartierfest.backend.benutzer;

// UC-015: Benutzer verwalten (AUTH-002)

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.partei.ParteiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BenutzerService {

    private final BenutzerRepository benutzerRepository;
    private final ParteiRepository parteiRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<BenutzerResponse> findAll() {
        return benutzerRepository.findAll().stream().map(BenutzerResponse::von).toList();
    }

    @Transactional
    public BenutzerResponse create(BenutzerRequest request) {
        if (benutzerRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Für diese E-Mail-Adresse existiert bereits ein Account.");
        }
        if (request.rolle() == Benutzer.Rolle.PARTEI && request.parteiId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Für die Rolle PARTEI muss eine Partei zugeordnet werden.");
        }
        Benutzer benutzer = new Benutzer();
        benutzer.setEmail(request.email());
        benutzer.setRolle(request.rolle());
        benutzer.setPartei(request.parteiId() == null ? null
                : Referenzen.aufloesen(parteiRepository, request.parteiId(), "Partei"));
        benutzer.setPasswortHash(passwordEncoder.encode(request.passwort()));
        return BenutzerResponse.von(benutzerRepository.save(benutzer));
    }

    @Transactional
    public void delete(Long id) {
        Benutzer benutzer = Referenzen.laden(benutzerRepository, id, "Benutzer");
        if (benutzer.getRolle() == Benutzer.Rolle.ORGANISATOR
                && benutzerRepository.countByRolle(Benutzer.Rolle.ORGANISATOR) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Der letzte Organisator-Account kann nicht gelöscht werden.");
        }
        benutzerRepository.deleteById(id);
        benutzerRepository.flush();
    }

    @Transactional
    public BenutzerResponse passwortSetzen(Long id, String neuesPasswort) {
        Benutzer benutzer = Referenzen.laden(benutzerRepository, id, "Benutzer");
        benutzer.setPasswortHash(passwordEncoder.encode(neuesPasswort));
        return BenutzerResponse.von(benutzerRepository.save(benutzer));
    }
}
