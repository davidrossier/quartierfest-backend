package ch.quartierfest.backend.auth;

// UC-014 / SEC-002: Brute-Force-Drosselung für POST /api/auth/login

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Zählt fehlgeschlagene Login-Versuche pro E-Mail-Adresse und pro Client-IP im Speicher
 * (Single-Instance-Deployment). Ist ein Limit erreicht, gilt der Schlüssel für die
 * Sperrdauer als gesperrt; jeder weitere Fehlversuch verlängert die Sperre. Ein
 * erfolgreicher Login löscht beide Zähler. Ein Limit von 0 deaktiviert die jeweilige Prüfung.
 */
@Component
public class LoginDrosselung {

    private final int maxFehlversucheEmail;
    private final int maxFehlversucheIp;
    private final Cache<String, Integer> fehlversuche;

    @Autowired
    public LoginDrosselung(@Value("${auth.drosselung.max-fehlversuche-email:5}") int maxFehlversucheEmail,
                           @Value("${auth.drosselung.max-fehlversuche-ip:20}") int maxFehlversucheIp,
                           @Value("${auth.drosselung.sperre-minuten:15}") long sperreMinuten) {
        this(maxFehlversucheEmail, maxFehlversucheIp, Duration.ofMinutes(sperreMinuten), Ticker.systemTicker());
    }

    LoginDrosselung(int maxFehlversucheEmail, int maxFehlversucheIp, Duration sperre, Ticker ticker) {
        this.maxFehlversucheEmail = maxFehlversucheEmail;
        this.maxFehlversucheIp = maxFehlversucheIp;
        this.fehlversuche = Caffeine.newBuilder()
                .expireAfterWrite(sperre)
                .ticker(ticker)
                .build();
    }

    public boolean istGesperrt(String email, String clientIp) {
        return limitErreicht(emailKey(email), maxFehlversucheEmail)
                || limitErreicht(ipKey(clientIp), maxFehlversucheIp);
    }

    public void fehlversuch(String email, String clientIp) {
        fehlversuche.asMap().merge(emailKey(email), 1, Integer::sum);
        fehlversuche.asMap().merge(ipKey(clientIp), 1, Integer::sum);
    }

    public void erfolg(String email, String clientIp) {
        fehlversuche.invalidate(emailKey(email));
        fehlversuche.invalidate(ipKey(clientIp));
    }

    /** Löscht alle Zähler — für Tests, die den geteilten Spring-Context sauber hinterlassen müssen. */
    public void zuruecksetzen() {
        fehlversuche.invalidateAll();
    }

    private boolean limitErreicht(String key, int limit) {
        if (limit <= 0) {
            return false;
        }
        Integer anzahl = fehlversuche.getIfPresent(key);
        return anzahl != null && anzahl >= limit;
    }

    private static String emailKey(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private static String ipKey(String clientIp) {
        return "ip:" + (clientIp == null ? "" : clientIp);
    }
}
