package ch.quartierfest.backend.zahlung;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** UC-013: Request für POST /api/zahlungen (API-001 Stufe 2). */
public record ZahlungRequest(
        @NotNull Long abrechnungId,
        @NotNull Zahlung.Zahlungskanal zahlungskanal,
        @NotNull LocalDate datum,
        @NotNull BigDecimal betrag) {
}
