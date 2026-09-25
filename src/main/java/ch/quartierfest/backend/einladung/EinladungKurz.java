package ch.quartierfest.backend.einladung;

import ch.quartierfest.backend.event.EventResponse;
import ch.quartierfest.backend.partei.ParteiKurz;
import org.jspecify.annotations.Nullable;

/**
 * Einladung als eingebettete Referenz (Teilnahme, Konsumation, Abrechnung) — nur die Felder, die das
 * Frontend über {@code teilnahme.einladung.*} liest (API-001 Stufe 2, E3).
 */
public record EinladungKurz(
        Long id,
        Einladung.EinladungStatus status,
        @Nullable Integer anzahlPersonen,
        EventResponse event,
        ParteiKurz partei) {

    public static EinladungKurz von(Einladung einladung) {
        return new EinladungKurz(einladung.getId(), einladung.getStatus(), einladung.getAnzahlPersonen(),
                EventResponse.von(einladung.getEvent()), ParteiKurz.von(einladung.getPartei()));
    }
}
