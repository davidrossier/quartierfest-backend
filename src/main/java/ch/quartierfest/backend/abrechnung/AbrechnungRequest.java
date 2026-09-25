package ch.quartierfest.backend.abrechnung;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

/** UC-011: Request für POST /api/abrechnungen (API-001 Stufe 2). Berechnung im Backend folgt mit BIZ-001. */
public record AbrechnungRequest(
        @NotNull Long teilnahmeId,
        @NotNull BigDecimal anteilAllgemeinkosten,
        @NotNull BigDecimal totalKonsumation,
        @NotNull BigDecimal totalBetrag,
        @NotNull Abrechnung.Zustellungskanal zustellungskanal,
        @Nullable LocalDate zustellungsDatum) {
}
