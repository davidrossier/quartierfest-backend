package ch.quartierfest.backend.allgemeinausgabe;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.event.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AllgemeinausgabeService {

    private final AllgemeinausgabeRepository allgemeinausgabeRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<AllgemeinausgabeResponse> findAll() {
        return allgemeinausgabeRepository.findAll().stream().map(AllgemeinausgabeResponse::von).toList();
    }

    @Transactional
    public AllgemeinausgabeResponse create(AllgemeinausgabeRequest request) {
        Allgemeinausgabe ausgabe = new Allgemeinausgabe();
        uebernehmen(ausgabe, request);
        return AllgemeinausgabeResponse.von(allgemeinausgabeRepository.save(ausgabe));
    }

    /** UC-007: Ausgabe bearbeiten (REST-003, erweitert) — unbekannte id → 404. */
    @Transactional
    public AllgemeinausgabeResponse update(Long id, AllgemeinausgabeRequest request) {
        Allgemeinausgabe ausgabe = Referenzen.laden(allgemeinausgabeRepository, id, "Allgemeinausgabe");
        uebernehmen(ausgabe, request);
        return AllgemeinausgabeResponse.von(allgemeinausgabeRepository.saveAndFlush(ausgabe));
    }

    @Transactional
    public void delete(Long id) {
        allgemeinausgabeRepository.deleteById(id);
        allgemeinausgabeRepository.flush();
    }

    private void uebernehmen(Allgemeinausgabe ausgabe, AllgemeinausgabeRequest request) {
        ausgabe.setEvent(Referenzen.aufloesen(eventRepository, request.eventId(), "Event"));
        ausgabe.setBeschreibung(request.beschreibung());
        ausgabe.setHerkunft(request.herkunft());
        ausgabe.setBetrag(request.betrag());
    }
}
