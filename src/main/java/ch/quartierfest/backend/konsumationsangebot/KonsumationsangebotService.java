package ch.quartierfest.backend.konsumationsangebot;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.event.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KonsumationsangebotService {

    private final KonsumationsangebotRepository konsumationsangebotRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<KonsumationsangebotResponse> findAll() {
        return konsumationsangebotRepository.findAll().stream().map(KonsumationsangebotResponse::von).toList();
    }

    @Transactional
    public KonsumationsangebotResponse create(KonsumationsangebotRequest request) {
        Konsumationsangebot angebot = new Konsumationsangebot();
        uebernehmen(angebot, request);
        return KonsumationsangebotResponse.von(konsumationsangebotRepository.save(angebot));
    }

    /** UC-008: Angebot bearbeiten (REST-003, erweitert) — unbekannte id → 404. */
    @Transactional
    public KonsumationsangebotResponse update(Long id, KonsumationsangebotRequest request) {
        Konsumationsangebot angebot = Referenzen.laden(konsumationsangebotRepository, id, "Konsumationsangebot");
        uebernehmen(angebot, request);
        return KonsumationsangebotResponse.von(konsumationsangebotRepository.saveAndFlush(angebot));
    }

    @Transactional
    public void delete(Long id) {
        konsumationsangebotRepository.deleteById(id);
        konsumationsangebotRepository.flush();
    }

    private void uebernehmen(Konsumationsangebot angebot, KonsumationsangebotRequest request) {
        angebot.setEvent(Referenzen.aufloesen(eventRepository, request.eventId(), "Event"));
        angebot.setBezeichnung(request.bezeichnung());
        angebot.setPreis(request.preis());
    }
}
