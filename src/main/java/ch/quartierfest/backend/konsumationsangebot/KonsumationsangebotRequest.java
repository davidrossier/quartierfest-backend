package ch.quartierfest.backend.konsumationsangebot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** UC-008: Request für POST und PUT /api/konsumationsangebote (API-001 Stufe 2). */
public record KonsumationsangebotRequest(
        @NotNull Long eventId,
        @NotBlank String bezeichnung,
        @NotNull BigDecimal preis) {
}
