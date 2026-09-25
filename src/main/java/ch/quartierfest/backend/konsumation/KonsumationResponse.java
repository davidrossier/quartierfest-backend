package ch.quartierfest.backend.konsumation;

import ch.quartierfest.backend.konsumationsangebot.KonsumationsangebotKurz;
import ch.quartierfest.backend.teilnahme.TeilnahmeKurz;

/** UC-010: Konsumation in Antworten (API-001 Stufe 2). */
public record KonsumationResponse(
        Long id,
        TeilnahmeKurz teilnahme,
        KonsumationsangebotKurz konsumationsangebot,
        Integer anzahl) {

    public static KonsumationResponse von(Konsumation konsumation) {
        return new KonsumationResponse(konsumation.getId(), TeilnahmeKurz.von(konsumation.getTeilnahme()),
                KonsumationsangebotKurz.von(konsumation.getKonsumationsangebot()), konsumation.getAnzahl());
    }
}
