package ch.quartierfest.backend.konsumation;

import jakarta.validation.constraints.NotNull;

/** UC-010: Request für POST /api/konsumationen (API-001 Stufe 2). */
public record KonsumationRequest(
        @NotNull Long teilnahmeId,
        @NotNull Long konsumationsangebotId,
        @NotNull Integer anzahl) {
}
