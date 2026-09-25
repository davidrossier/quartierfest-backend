package ch.quartierfest.backend.person;

import ch.quartierfest.backend.Referenzen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll() {
        return personRepository.findAll().stream().map(PersonResponse::von).toList();
    }

    @Transactional
    public PersonResponse create(PersonRequest request) {
        Person person = new Person();
        uebernehmen(person, request);
        return PersonResponse.von(personRepository.save(person));
    }

    /** REST-002: unbekannte id → 404 statt stillem Anlegen. */
    @Transactional
    public PersonResponse update(Long id, PersonRequest request) {
        Person person = Referenzen.laden(personRepository, id, "Person");
        uebernehmen(person, request);
        return PersonResponse.von(personRepository.saveAndFlush(person));
    }

    @Transactional
    public void delete(Long id) {
        personRepository.deleteById(id);
        personRepository.flush();
    }

    private static void uebernehmen(Person person, PersonRequest request) {
        person.setVorname(request.vorname());
        person.setName(request.name());
        person.setTelefonnummer(request.telefonnummer());
        person.setMobilenummer(request.mobilenummer());
        person.setEmail(request.email());
    }
}
