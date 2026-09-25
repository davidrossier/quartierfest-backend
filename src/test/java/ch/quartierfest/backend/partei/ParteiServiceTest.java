package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.person.Person;
import ch.quartierfest.backend.person.PersonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for UC-002 – Parteien verwalten (Service-Logik). */
@ExtendWith(MockitoExtension.class)
class ParteiServiceTest {

    @Mock
    private ParteiRepository parteiRepository;

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private ParteiService parteiService;

    private static Person person(long id) {
        Person p = new Person();
        p.setId(id);
        p.setVorname("Vorname " + id);
        p.setName("Name " + id);
        return p;
    }

    private static ParteiRequest request(List<Long> personenIds) {
        return new ParteiRequest("Familie Müller", "Musterstrasse 1", false, null, personenIds);
    }

    @Test
    @DisplayName("UC-002: create() mit personenIds löst IDs zu Person-Objekten auf")
    void create_withPersonenIds_resolvesPersonen() {
        Person p1 = person(1L);
        Person p2 = person(2L);
        when(personRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(p1, p2));
        when(parteiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ParteiResponse response = parteiService.create(request(List.of(1L, 2L)));

        ArgumentCaptor<Partei> gespeichert = ArgumentCaptor.forClass(Partei.class);
        verify(parteiRepository).save(gespeichert.capture());
        assertThat(gespeichert.getValue().getPersonen()).containsExactly(p1, p2);
        assertThat(response.personen()).extracting("id").containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("UC-002: create() ohne personenIds setzt leere Personenliste")
    void create_withNullPersonenIds_setsEmptyList() {
        when(parteiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ParteiResponse response = parteiService.create(request(null));

        assertThat(response.personen()).isEmpty();
        verify(personRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("UC-002: create() mit unbekannter Personen-ID wirft 400 (API-001 Stufe 2, E5)")
    void create_unbekanntePerson_wirft400() {
        when(personRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(person(1L)));

        assertThatThrownBy(() -> parteiService.create(request(List.of(1L, 99L))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(parteiRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC-002: update() auf unbekannte id wirft 404 statt anzulegen (REST-002)")
    void update_unbekannteId_wirft404() {
        when(parteiRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parteiService.update(42L, request(null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(parteiRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("UC-002: findAll() bildet Parteien auf Responses ab")
    void findAll_mapsToResponse() {
        Partei p = new Partei();
        p.setId(1L);
        p.setBezeichnung("Familie Müller");
        p.setAdresse("Musterstrasse 1");
        when(parteiRepository.findAll()).thenReturn(List.of(p));

        List<ParteiResponse> result = parteiService.findAll();

        assertThat(result).extracting(ParteiResponse::bezeichnung).containsExactly("Familie Müller");
    }

    @Test
    @DisplayName("UC-002: delete() löscht Partei nach ID")
    void delete_delegatesToRepository() {
        parteiService.delete(1L);

        verify(parteiRepository).deleteById(1L);
    }
}
