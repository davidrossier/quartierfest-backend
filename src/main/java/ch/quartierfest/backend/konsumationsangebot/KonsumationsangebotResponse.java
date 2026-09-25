package ch.quartierfest.backend.konsumationsangebot;

import ch.quartierfest.backend.event.EventResponse;

import java.math.BigDecimal;

/** UC-008: Konsumationsangebot in Antworten (API-001 Stufe 2). */
public record KonsumationsangebotResponse(
        Long id,
        EventResponse event,
        String bezeichnung,
        BigDecimal preis) {

    public static KonsumationsangebotResponse von(Konsumationsangebot angebot) {
        return new KonsumationsangebotResponse(angebot.getId(), EventResponse.von(angebot.getEvent()),
                angebot.getBezeichnung(), angebot.getPreis());
    }
}
