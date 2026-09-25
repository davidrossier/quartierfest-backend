package ch.quartierfest.backend.event;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;

/** UC-003: Event in Antworten, auch als eingebettete Referenz (API-001 Stufe 2). */
public record EventResponse(
        Long id,
        LocalDate datum,
        LocalTime startzeit,
        String standort,
        @Nullable String alternativerStandort,
        @Nullable LocalTime zeitAufstellen,
        @Nullable LocalTime zeitAufraumen) {

    public static EventResponse von(Event event) {
        return new EventResponse(event.getId(), event.getDatum(), event.getStartzeit(), event.getStandort(),
                event.getAlternativerStandort(), event.getZeitAufstellen(), event.getZeitAufraumen());
    }
}
