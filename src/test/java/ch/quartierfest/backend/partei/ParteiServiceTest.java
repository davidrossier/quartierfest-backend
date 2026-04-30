package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.person.Person;
import ch.quartierfest.backend.person.PersonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    @DisplayName("UC-002: save() mit personenIds löst IDs zu Person-Objekten auf")
    void save_withPersonenIds_resolvesPersonen() {
        Person p1 = new Person(); p1.setId(1L);
        Person p2 = new Person(); p2.setId(2L);

        Partei partei = new Partei();
        partei.setPersonenIds(List.of(1L, 2L));

        when(personRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(p1, p2));
        when(parteiRepository.save(any())).thenReturn(partei);

        parteiService.save(partei);

        assertThat(partei.getPersonen()).containsExactly(p1, p2);
        verify(personRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    @DisplayName("UC-002: save() ohne personenIds setzt leere Personenliste")
    void save_withNullPersonenIds_setsEmptyList() {
        Partei partei = new Partei();
        partei.setPersonenIds(null);
        when(parteiRepository.save(any())).thenReturn(partei);

        parteiService.save(partei);

        assertThat(partei.getPersonen()).isEmpty();
        verify(personRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("UC-002: findAll() delegiert an Repository")
    void findAll_delegatesToRepository() {
        Partei p = new Partei();
        when(parteiRepository.findAll()).thenReturn(List.of(p));

        List<Partei> result = parteiService.findAll();

        assertThat(result).containsExactly(p);
    }

    @Test
    @DisplayName("UC-002: delete() löscht Partei nach ID")
    void delete_delegatesToRepository() {
        parteiService.delete(1L);

        verify(parteiRepository).deleteById(1L);
    }
}
