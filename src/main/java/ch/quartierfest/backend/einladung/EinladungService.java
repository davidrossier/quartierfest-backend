package ch.quartierfest.backend.einladung;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.event.EventRepository;
import ch.quartierfest.backend.partei.ParteiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EinladungService {

    private final EinladungRepository einladungRepository;
    private final EventRepository eventRepository;
    private final ParteiRepository parteiRepository;

    @Transactional(readOnly = true)
    public List<EinladungResponse> findAll() {
        return einladungRepository.findAll().stream().map(EinladungResponse::von).toList();
    }

    @Transactional
    public EinladungResponse create(EinladungRequest request) {
        Einladung einladung = new Einladung();
        einladung.setEvent(Referenzen.aufloesen(eventRepository, request.eventId(), "Event"));
        einladung.setPartei(Referenzen.aufloesen(parteiRepository, request.parteiId(), "Partei"));
        einladung.setStatus(request.status());
        einladung.setAnzahlPersonen(request.anzahlPersonen());
        einladung.setHilftAufstellen(request.hilftAufstellen());
        einladung.setHilftAufraumen(request.hilftAufraumen());
        einladung.setBuffetBeitrag(request.buffetBeitrag());
        einladung.setBuffetBeitragBeschreibung(request.buffetBeitragBeschreibung());
        einladung.setBestaetigungVersendet(request.bestaetigungVersendet());
        return EinladungResponse.von(einladungRepository.save(einladung));
    }

    /** UC-004/UC-006: Rückmeldung erfassen, Bestätigung markieren (REST-003) — unbekannte id → 404. */
    @Transactional
    public EinladungResponse update(Long id, EinladungUpdateRequest request) {
        Einladung einladung = Referenzen.laden(einladungRepository, id, "Einladung");
        einladung.setStatus(request.status());
        einladung.setAnzahlPersonen(request.anzahlPersonen());
        einladung.setHilftAufstellen(request.hilftAufstellen());
        einladung.setHilftAufraumen(request.hilftAufraumen());
        einladung.setBuffetBeitrag(request.buffetBeitrag());
        einladung.setBuffetBeitragBeschreibung(request.buffetBeitragBeschreibung());
        einladung.setBestaetigungVersendet(request.bestaetigungVersendet());
        return EinladungResponse.von(einladungRepository.saveAndFlush(einladung));
    }

    @Transactional
    public void delete(Long id) {
        einladungRepository.deleteById(id);
        einladungRepository.flush();
    }
}
