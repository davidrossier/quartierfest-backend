package ch.quartierfest.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/** SEC-002: reiner Unit-Test der In-Memory-Drosselung mit gestellter Uhr. */
class LoginDrosselungTest {

    private final AtomicLong nanos = new AtomicLong();
    private LoginDrosselung drosselung;

    @BeforeEach
    void setUp() {
        drosselung = new LoginDrosselung(3, 5, Duration.ofMinutes(15), nanos::get);
    }

    private void vorspulen(Duration d) {
        nanos.addAndGet(d.toNanos());
    }

    @Test
    @DisplayName("SEC-002: E-Mail ist nach Erreichen des Limits gesperrt, andere E-Mail nicht")
    void email_nachLimit_gesperrt() {
        drosselung.fehlversuch("a@quartier.ch", "10.0.0.1");
        drosselung.fehlversuch("a@quartier.ch", "10.0.0.1");
        assertThat(drosselung.istGesperrt("a@quartier.ch", "10.0.0.1")).isFalse();

        drosselung.fehlversuch("a@quartier.ch", "10.0.0.1");
        assertThat(drosselung.istGesperrt("a@quartier.ch", "10.0.0.1")).isTrue();
        assertThat(drosselung.istGesperrt("b@quartier.ch", "10.0.0.2")).isFalse();
    }

    @Test
    @DisplayName("SEC-002: E-Mail-Schlüssel ignoriert Gross-/Kleinschreibung und Leerzeichen")
    void email_normalisiert() {
        drosselung.fehlversuch("A@Quartier.ch", "10.0.0.1");
        drosselung.fehlversuch(" a@quartier.ch ", "10.0.0.1");
        drosselung.fehlversuch("a@QUARTIER.CH", "10.0.0.1");
        assertThat(drosselung.istGesperrt("a@quartier.ch", "10.0.0.9")).isTrue();
    }

    @Test
    @DisplayName("SEC-002: IP ist nach Erreichen des IP-Limits gesperrt, unabhängig von der E-Mail")
    void ip_nachLimit_gesperrt() {
        for (int i = 0; i < 5; i++) {
            drosselung.fehlversuch("user" + i + "@quartier.ch", "10.0.0.1");
        }
        assertThat(drosselung.istGesperrt("neu@quartier.ch", "10.0.0.1")).isTrue();
        assertThat(drosselung.istGesperrt("neu@quartier.ch", "10.0.0.2")).isFalse();
    }

    @Test
    @DisplayName("SEC-002: Sperre läuft nach der Sperrdauer ab")
    void sperre_laeuftAb() {
        for (int i = 0; i < 3; i++) {
            drosselung.fehlversuch("a@quartier.ch", "10.0.0.1");
        }
        vorspulen(Duration.ofMinutes(14));
        assertThat(drosselung.istGesperrt("a@quartier.ch", "10.0.0.1")).isTrue();

        vorspulen(Duration.ofMinutes(2));
        assertThat(drosselung.istGesperrt("a@quartier.ch", "10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("SEC-002: Erfolgreicher Login setzt E-Mail- und IP-Zähler zurück")
    void erfolg_setztZurueck() {
        drosselung.fehlversuch("a@quartier.ch", "10.0.0.1");
        drosselung.fehlversuch("a@quartier.ch", "10.0.0.1");
        drosselung.erfolg("a@quartier.ch", "10.0.0.1");

        drosselung.fehlversuch("a@quartier.ch", "10.0.0.1");
        drosselung.fehlversuch("a@quartier.ch", "10.0.0.1");
        assertThat(drosselung.istGesperrt("a@quartier.ch", "10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("SEC-002: Limit 0 deaktiviert die jeweilige Prüfung")
    void limitNull_deaktiviert() {
        LoginDrosselung ohneIp = new LoginDrosselung(3, 0, Duration.ofMinutes(15), nanos::get);
        for (int i = 0; i < 50; i++) {
            ohneIp.fehlversuch("user" + i + "@quartier.ch", "10.0.0.1");
        }
        assertThat(ohneIp.istGesperrt("neu@quartier.ch", "10.0.0.1")).isFalse();
    }
}
