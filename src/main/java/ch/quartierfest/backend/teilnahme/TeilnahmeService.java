package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.benutzer.Benutzer;
import ch.quartierfest.backend.benutzer.BenutzerRepository;
import ch.quartierfest.backend.einladung.EinladungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeilnahmeService {

    private final TeilnahmeRepository teilnahmeRepository;
    private final EinladungRepository einladungRepository;
    private final BenutzerRepository benutzerRepository;

    @Transactional(readOnly = true)
    public List<TeilnahmeResponse> findAll() {
        return teilnahmeRepository.findAll().stream().map(TeilnahmeResponse::von).toList();
    }

    @Transactional
    public TeilnahmeResponse create(TeilnahmeRequest request) {
        Teilnahme teilnahme = new Teilnahme();
        teilnahme.setEinladung(Referenzen.aufloesen(einladungRepository, request.einladungId(), "Einladung"));
        teilnahme.setAnzahlPersonenEffektiv(request.anzahlPersonenEffektiv());
        teilnahme.setHilftAufstellen(request.hilftAufstellen());
        teilnahme.setHilftAufraumen(request.hilftAufraumen());
        if (request.buffetBeitraege() != null) {
            teilnahme.getBuffetBeitraege().addAll(request.buffetBeitraege());
        }
        return TeilnahmeResponse.von(teilnahmeRepository.save(teilnahme));
    }

    @Transactional
    public void delete(Long id) {
        teilnahmeRepository.deleteById(id);
        teilnahmeRepository.flush();
    }

    /**
     * UC-016: Teilnahme der eigenen Partei zum nächsten Event
     * (frühestes Event-Datum >= heute), ermittelt via JWT sub → Benutzer → Partei.
     */
    @Transactional(readOnly = true)
    public TeilnahmeResponse findMeine(String sub) {
        Benutzer benutzer = benutzerRepository.findById(parseSub(sub))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Kein Benutzer zur Anmeldung gefunden."));
        if (benutzer.getPartei() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Dem Benutzer ist keine Partei zugeordnet.");
        }
        return teilnahmeRepository.findEigeneAbStichtag(benutzer.getPartei().getId(), LocalDate.now())
                .stream().findFirst()
                .map(TeilnahmeResponse::von)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Für Ihren Haushalt wurde noch keine Teilnahme erstellt."));
    }

    /** UC-016: Whitelist-Update — nur die vier PARTEI-editierbaren Felder, nie die Einladung. */
    @PreAuthorize("@teilnahmeZugriff.darfBearbeiten(#id, authentication)")
    @Transactional
    public TeilnahmeResponse update(Long id, TeilnahmeUpdateRequest request) {
        Teilnahme teilnahme = Referenzen.laden(teilnahmeRepository, id, "Teilnahme");
        teilnahme.setAnzahlPersonenEffektiv(request.anzahlPersonenEffektiv());
        teilnahme.setHilftAufstellen(request.hilftAufstellen());
        teilnahme.setHilftAufraumen(request.hilftAufraumen());
        teilnahme.getBuffetBeitraege().clear();
        if (request.buffetBeitraege() != null) {
            teilnahme.getBuffetBeitraege().addAll(request.buffetBeitraege());
        }
        return TeilnahmeResponse.von(teilnahmeRepository.saveAndFlush(teilnahme));
    }

    private Long parseSub(String sub) {
        try {
            return Long.valueOf(sub);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ungültige Anmeldung.");
        }
    }
}
