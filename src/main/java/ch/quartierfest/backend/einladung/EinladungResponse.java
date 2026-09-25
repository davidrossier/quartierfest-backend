package ch.quartierfest.backend.einladung;

import ch.quartierfest.backend.event.EventResponse;
import ch.quartierfest.backend.partei.ParteiKurz;
import org.jspecify.annotations.Nullable;

/** UC-004: Einladung in Antworten; Partei ohne Personenliste (API-001 Stufe 2). */
public record EinladungResponse(
        Long id,
        EventResponse event,
        ParteiKurz partei,
        Einladung.EinladungStatus status,
        @Nullable Integer anzahlPersonen,
        @Nullable Boolean hilftAufstellen,
        @Nullable Boolean hilftAufraumen,
        Einladung.@Nullable BuffetBeitrag buffetBeitrag,
        @Nullable String buffetBeitragBeschreibung,
        boolean bestaetigungVersendet) {

    public static EinladungResponse von(Einladung einladung) {
        return new EinladungResponse(einladung.getId(), EventResponse.von(einladung.getEvent()),
                ParteiKurz.von(einladung.getPartei()), einladung.getStatus(), einladung.getAnzahlPersonen(),
                einladung.getHilftAufstellen(), einladung.getHilftAufraumen(), einladung.getBuffetBeitrag(),
                einladung.getBuffetBeitragBeschreibung(), einladung.isBestaetigungVersendet());
    }
}
