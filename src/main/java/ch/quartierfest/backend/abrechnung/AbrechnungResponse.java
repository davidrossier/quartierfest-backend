package ch.quartierfest.backend.abrechnung;

import ch.quartierfest.backend.teilnahme.TeilnahmeKurz;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

/** UC-011/UC-012: Abrechnung in Antworten (API-001 Stufe 2). */
public record AbrechnungResponse(
        Long id,
        TeilnahmeKurz teilnahme,
        BigDecimal anteilAllgemeinkosten,
        BigDecimal totalKonsumation,
        BigDecimal totalBetrag,
        Abrechnung.Zustellungskanal zustellungskanal,
        @Nullable LocalDate zustellungsDatum) {

    public static AbrechnungResponse von(Abrechnung abrechnung) {
        return new AbrechnungResponse(abrechnung.getId(), TeilnahmeKurz.von(abrechnung.getTeilnahme()),
                abrechnung.getAnteilAllgemeinkosten(), abrechnung.getTotalKonsumation(), abrechnung.getTotalBetrag(),
                abrechnung.getZustellungskanal(), abrechnung.getZustellungsDatum());
    }
}
