package ch.quartierfest.backend.event;

import ch.quartierfest.backend.Referenzen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {
        return eventRepository.findAll().stream().map(EventResponse::von).toList();
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        Event event = new Event();
        uebernehmen(event, request);
        return EventResponse.von(eventRepository.save(event));
    }

    /** REST-002: unbekannte id → 404 statt stillem Anlegen. */
    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        Event event = Referenzen.laden(eventRepository, id, "Event");
        uebernehmen(event, request);
        return EventResponse.von(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public void delete(Long id) {
        eventRepository.deleteById(id);
        eventRepository.flush();
    }

    private static void uebernehmen(Event event, EventRequest request) {
        event.setDatum(request.datum());
        event.setStartzeit(request.startzeit());
        event.setStandort(request.standort());
        event.setAlternativerStandort(request.alternativerStandort());
        event.setZeitAufstellen(request.zeitAufstellen());
        event.setZeitAufraumen(request.zeitAufraumen());
    }
}
