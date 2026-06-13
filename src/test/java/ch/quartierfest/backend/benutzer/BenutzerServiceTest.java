package ch.quartierfest.backend.benutzer;

import ch.quartierfest.backend.partei.Partei;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
class BenutzerServiceTest {

    @Mock
    private BenutzerRepository benutzerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BenutzerService benutzerService;

    private Benutzer buildParteiBenutzer() {
        Partei partei = new Partei();
        partei.setId(1L);
        Benutzer benutzer = new Benutzer();
        benutzer.setEmail("mueller@quartier.ch");
        benutzer.setPasswort("geheim-1234");
        benutzer.setRolle(Benutzer.Rolle.PARTEI);
        benutzer.setPartei(partei);
        return benutzer;
    }

    @Test
    @DisplayName("UC-015: save() hasht das Passwort und leert das Klartextfeld")
    void save_hashtPasswort() {
        Benutzer benutzer = buildParteiBenutzer();
        when(benutzerRepository.existsByEmail("mueller@quartier.ch")).thenReturn(false);
        when(passwordEncoder.encode("geheim-1234")).thenReturn("$2a$hash");
        when(benutzerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Benutzer gespeichert = benutzerService.save(benutzer);

        assertThat(gespeichert.getPasswortHash()).isEqualTo("$2a$hash");
        assertThat(gespeichert.getPasswort()).isNull();
        verify(benutzerRepository).save(benutzer);
    }

    @Test
    @DisplayName("UC-015: save() mit bereits vergebener E-Mail wirft 409")
    void save_duplikatEmail_wirft409() {
        Benutzer benutzer = buildParteiBenutzer();
        when(benutzerRepository.existsByEmail("mueller@quartier.ch")).thenReturn(true);

        assertThatThrownBy(() -> benutzerService.save(benutzer))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(benutzerRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC-015: save() mit Rolle PARTEI ohne Partei wirft 400")
    void save_parteiOhnePartei_wirft400() {
        Benutzer benutzer = buildParteiBenutzer();
        benutzer.setPartei(null);
        when(benutzerRepository.existsByEmail("mueller@quartier.ch")).thenReturn(false);

        assertThatThrownBy(() -> benutzerService.save(benutzer))
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
        Benutzer benutzer = buildParteiBenutzer();
        benutzer.setId(8L);
        when(benutzerRepository.findById(8L)).thenReturn(Optional.of(benutzer));

        benutzerService.delete(8L);

        verify(benutzerRepository).deleteById(8L);
    }

    @Test
    @DisplayName("UC-015: passwortSetzen() ersetzt den Hash")
    void passwortSetzen_ersetztHash() {
        Benutzer benutzer = buildParteiBenutzer();
        benutzer.setId(9L);
        benutzer.setPasswortHash("$2a$alt");
        when(benutzerRepository.findById(9L)).thenReturn(Optional.of(benutzer));
        when(passwordEncoder.encode("neues-passwort")).thenReturn("$2a$neu");
        when(benutzerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Benutzer aktualisiert = benutzerService.passwortSetzen(9L, "neues-passwort");

        assertThat(aktualisiert.getPasswortHash()).isEqualTo("$2a$neu");
    }
}
