package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.EinladungKurz;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** UC-005/UC-016: Teilnahme in Antworten; Einladung als Kurzreferenz ohne Personenlisten (API-001 Stufe 2). */
public record TeilnahmeResponse(
        Long id,
        EinladungKurz einladung,
        @Nullable Integer anzahlPersonenEffektiv,
        @Nullable Boolean hilftAufstellen,
        @Nullable Boolean hilftAufraumen,
        List<TeilnahmeBuffetBeitrag> buffetBeitraege) {

    public static TeilnahmeResponse von(Teilnahme teilnahme) {
        return new TeilnahmeResponse(teilnahme.getId(), EinladungKurz.von(teilnahme.getEinladung()),
                teilnahme.getAnzahlPersonenEffektiv(), teilnahme.getHilftAufstellen(), teilnahme.getHilftAufraumen(),
                List.copyOf(teilnahme.getBuffetBeitraege()));
    }
}
