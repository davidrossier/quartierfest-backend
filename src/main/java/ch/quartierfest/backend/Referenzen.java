package ch.quartierfest.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * API-001 Stufe 2: Laden von Entities anhand einer ID aus einem Request.
 *
 * <ul>
 *   <li>{@link #aufloesen} — Referenz im Request-Body (z.B. {@code eventId}): unbekannte ID ist ungültige
 *       Eingabe → 400 (E5).</li>
 *   <li>{@link #laden} — Ressource im Pfad (z.B. {@code PUT /api/persons/{id}}): unbekannte ID → 404 (REST-002).</li>
 * </ul>
 */
public final class Referenzen {

    private Referenzen() {
    }

    public static <T> T aufloesen(JpaRepository<T, Long> repository, Long id, String bezeichnung) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Referenzierter Datensatz existiert nicht: " + bezeichnung + " " + id + "."));
    }

    public static <T> T laden(JpaRepository<T, Long> repository, Long id, String bezeichnung) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                bezeichnung + " nicht gefunden."));
    }
}
