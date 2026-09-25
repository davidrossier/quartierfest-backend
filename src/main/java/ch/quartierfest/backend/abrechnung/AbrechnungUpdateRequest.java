package ch.quartierfest.backend.abrechnung;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UC-011/UC-012: Whitelist für PUT /api/abrechnungen/{id} (REST-003) — Kanal und Zustelldatum (UC-012)
 * sowie die Beträge für die manuelle Übersteuerung. Die Teilnahme ist nicht änderbar.
 */
public record AbrechnungUpdateRequest(
        @NotNull BigDecimal anteilAllgemeinkosten,
        @NotNull BigDecimal totalKonsumation,
        @NotNull BigDecimal totalBetrag,
        @NotNull Abrechnung.Zustellungskanal zustellungskanal,
        @Nullable LocalDate zustellungsDatum) {
}
