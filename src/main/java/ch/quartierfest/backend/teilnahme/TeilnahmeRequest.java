package ch.quartierfest.backend.teilnahme;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * UC-005: Request für POST /api/teilnahmen (API-001 Stufe 2). Kein {@code id}-Feld: ein POST mit id
 * wird als unbekanntes Feld mit 400 abgelehnt (REST-001, TC-041); Updates laufen über den Whitelist-PUT.
 */
public record TeilnahmeRequest(
        @NotNull Long einladungId,
        @Nullable Integer anzahlPersonenEffektiv,
        @Nullable Boolean hilftAufstellen,
        @Nullable Boolean hilftAufraumen,
        @Valid @Nullable List<TeilnahmeBuffetBeitrag> buffetBeitraege) {
}
