package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.benutzer.Benutzer;
import ch.quartierfest.backend.benutzer.BenutzerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeilnahmeService {

    private final TeilnahmeRepository teilnahmeRepository;
    private final BenutzerRepository benutzerRepository;

    public List<Teilnahme> findAll() {
        return teilnahmeRepository.findAll();
    }

    public Teilnahme save(Teilnahme teilnahme) {
        return teilnahmeRepository.save(teilnahme);
    }

    public void delete(Long id) {
        teilnahmeRepository.deleteById(id);
    }

    /**
     * UC-016: Teilnahme der eigenen Partei zum nächsten Event
     * (frühestes Event-Datum >= heute), ermittelt via JWT sub → Benutzer → Partei.
     */
    public Teilnahme findMeine(String sub) {
        Benutzer benutzer = benutzerRepository.findById(parseSub(sub))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Kein Benutzer zur Anmeldung gefunden."));
        if (benutzer.getPartei() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Dem Benutzer ist keine Partei zugeordnet.");
        }
        return teilnahmeRepository.findEigeneAbStichtag(benutzer.getPartei().getId(), LocalDate.now())
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Für Ihren Haushalt wurde noch keine Teilnahme erstellt."));
    }

    /** UC-016: Whitelist-Update — nur die vier PARTEI-editierbaren Felder, nie die Einladung. */
    @PreAuthorize("@teilnahmeZugriff.darfBearbeiten(#id, authentication)")
    public Teilnahme update(Long id, TeilnahmeUpdateRequest request) {
        Teilnahme teilnahme = teilnahmeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Teilnahme nicht gefunden."));
        teilnahme.setAnzahlPersonenEffektiv(request.anzahlPersonenEffektiv());
        teilnahme.setHilftAufstellen(request.hilftAufstellen());
        teilnahme.setHilftAufraumen(request.hilftAufraumen());
        teilnahme.getBuffetBeitraege().clear();
        if (request.buffetBeitraege() != null) {
            teilnahme.getBuffetBeitraege().addAll(request.buffetBeitraege());
        }
        return teilnahmeRepository.save(teilnahme);
    }

    private Long parseSub(String sub) {
        try {
            return Long.valueOf(sub);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ungültige Anmeldung.");
        }
    }
}
