package ch.quartierfest.backend.benutzer;

import ch.quartierfest.backend.partei.Partei;
import ch.quartierfest.backend.partei.ParteiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for UC-015 – Benutzer verwalten (Service-Logik). */
@ExtendWith(MockitoExtension.class)
class BenutzerServiceTest {

    @Mock
    private BenutzerRepository benutzerRepository;

    @Mock
    private ParteiRepository parteiRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BenutzerService benutzerService;

    private static Partei partei() {
        Partei partei = new Partei();
        partei.setId(1L);
        partei.setBezeichnung("Familie Müller");
        partei.setAdresse("Musterstrasse 1");
        return partei;
    }

    private static BenutzerRequest parteiRequest(Long parteiId) {
        return new BenutzerRequest("mueller@quartier.ch", "geheim-1234", Benutzer.Rolle.PARTEI, parteiId);
    }

    @Test
    @DisplayName("UC-015: create() speichert nur den Hash, die Antwort enthält kein Passwort")
    void create_hashtPasswort() {
        when(benutzerRepository.existsByEmail("mueller@quartier.ch")).thenReturn(false);
        when(parteiRepository.findById(1L)).thenReturn(Optional.of(partei()));
        when(passwordEncoder.encode("geheim-1234")).thenReturn("$2a$hash");
        when(benutzerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BenutzerResponse response = benutzerService.create(parteiRequest(1L));

        ArgumentCaptor<Benutzer> gespeichert = ArgumentCaptor.forClass(Benutzer.class);
        verify(benutzerRepository).save(gespeichert.capture());
        assertThat(gespeichert.getValue().getPasswortHash()).isEqualTo("$2a$hash");
        assertThat(response.partei()).isNotNull();
        assertThat(response.partei().bezeichnung()).isEqualTo("Familie Müller");
    }

    @Test
    @DisplayName("UC-015: create() mit bereits vergebener E-Mail wirft 409")
    void create_duplikatEmail_wirft409() {
        when(benutzerRepository.existsByEmail("mueller@quartier.ch")).thenReturn(true);

        assertThatThrownBy(() -> benutzerService.create(parteiRequest(1L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(benutzerRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC-015: create() mit Rolle PARTEI ohne Partei wirft 400")
    void create_parteiOhnePartei_wirft400() {
        when(benutzerRepository.existsByEmail("mueller@quartier.ch")).thenReturn(false);

        assertThatThrownBy(() -> benutzerService.create(parteiRequest(null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(benutzerRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC-015: create() mit unbekannter Partei wirft 400 (API-001 Stufe 2, E5)")
    void create_unbekanntePartei_wirft400() {
        when(benutzerRepository.existsByEmail("mueller@quartier.ch")).thenReturn(false);
        when(parteiRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benutzerService.create(parteiRequest(99L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(benutzerRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC-015: delete() auf letzten ORGANISATOR wirft 409")
    void delete_letzterOrganisator_wirft409() {
        Benutzer orga = new Benutzer();
        orga.setId(7L);
        orga.setRolle(Benutzer.Rolle.ORGANISATOR);
        when(benutzerRepository.findById(7L)).thenReturn(Optional.of(orga));
        when(benutzerRepository.countByRolle(Benutzer.Rolle.ORGANISATOR)).thenReturn(1L);

        assertThatThrownBy(() -> benutzerService.delete(7L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(benutzerRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("UC-015: delete() auf PARTEI-Benutzer löscht")
    void delete_parteiBenutzer_loescht() {
        Benutzer benutzer = new Benutzer();
        benutzer.setId(8L);
        benutzer.setRolle(Benutzer.Rolle.PARTEI);
        when(benutzerRepository.findById(8L)).thenReturn(Optional.of(benutzer));

        benutzerService.delete(8L);

        verify(benutzerRepository).deleteById(8L);
    }

    @Test
    @DisplayName("UC-015: passwortSetzen() ersetzt den Hash")
    void passwortSetzen_ersetztHash() {
        Benutzer benutzer = new Benutzer();
        benutzer.setId(9L);
        benutzer.setEmail("mueller@quartier.ch");
        benutzer.setRolle(Benutzer.Rolle.PARTEI);
        benutzer.setPasswortHash("$2a$alt");
        when(benutzerRepository.findById(9L)).thenReturn(Optional.of(benutzer));
        when(passwordEncoder.encode("neues-passwort")).thenReturn("$2a$neu");
        when(benutzerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BenutzerResponse response = benutzerService.passwortSetzen(9L, "neues-passwort");

        assertThat(benutzer.getPasswortHash()).isEqualTo("$2a$neu");
        assertThat(response.id()).isEqualTo(9L);
    }
}
