package ch.quartierfest.backend.allgemeinausgabe;

import ch.quartierfest.backend.event.EventResponse;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/** UC-007: Allgemeinausgabe in Antworten (API-001 Stufe 2). */
public record AllgemeinausgabeResponse(
        Long id,
        EventResponse event,
        String beschreibung,
        @Nullable String herkunft,
        BigDecimal betrag) {

    public static AllgemeinausgabeResponse von(Allgemeinausgabe ausgabe) {
        return new AllgemeinausgabeResponse(ausgabe.getId(), EventResponse.von(ausgabe.getEvent()),
                ausgabe.getBeschreibung(), ausgabe.getHerkunft(), ausgabe.getBetrag());
    }
}
