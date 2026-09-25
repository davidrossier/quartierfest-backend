package ch.quartierfest.backend.konsumationsangebot;

import java.math.BigDecimal;

/** UC-008/UC-010: Konsumationsangebot als eingebettete Referenz, z.B. in einer Konsumation (API-001 Stufe 2). */
public record KonsumationsangebotKurz(
        Long id,
        String bezeichnung,
        BigDecimal preis) {

    public static KonsumationsangebotKurz von(Konsumationsangebot angebot) {
        return new KonsumationsangebotKurz(angebot.getId(), angebot.getBezeichnung(), angebot.getPreis());
    }
}
