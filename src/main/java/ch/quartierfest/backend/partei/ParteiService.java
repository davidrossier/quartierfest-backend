package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.person.Person;
import ch.quartierfest.backend.person.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ParteiService {

    private final ParteiRepository parteiRepository;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public List<ParteiResponse> findAll() {
        return parteiRepository.findAll().stream().map(ParteiResponse::von).toList();
    }

    @Transactional
    public ParteiResponse create(ParteiRequest request) {
        Partei partei = new Partei();
        uebernehmen(partei, request);
        return ParteiResponse.von(parteiRepository.save(partei));
    }

    /** REST-002: unbekannte id → 404 statt stillem Anlegen. */
    @Transactional
    public ParteiResponse update(Long id, ParteiRequest request) {
        Partei partei = Referenzen.laden(parteiRepository, id, "Partei");
        uebernehmen(partei, request);
        return ParteiResponse.von(parteiRepository.saveAndFlush(partei));
    }

    @Transactional
    public void delete(Long id) {
        parteiRepository.deleteById(id);
        parteiRepository.flush();
    }

    private void uebernehmen(Partei partei, ParteiRequest request) {
        partei.setBezeichnung(request.bezeichnung());
        partei.setAdresse(request.adresse());
        partei.setTwintAktiv(request.twintAktiv());
        partei.setTwintMobilenummer(request.twintMobilenummer());
        partei.setPersonen(personenAufloesen(request.personenIds()));
    }

    /** API-001 Stufe 2 (E5): unbekannte Personen-IDs werden nicht mehr still verworfen, sondern → 400. */
    private List<Person> personenAufloesen(List<Long> personenIds) {
        if (personenIds == null || personenIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> ids = new LinkedHashSet<>(personenIds);
        List<Person> personen = personRepository.findAllById(ids);
        if (personen.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Referenzierter Datensatz existiert nicht: mindestens eine Person aus personenIds.");
        }
        return new ArrayList<>(personen);
    }
}
