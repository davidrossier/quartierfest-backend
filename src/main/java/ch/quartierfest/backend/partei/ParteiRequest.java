package ch.quartierfest.backend.partei;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** UC-002: Request für POST und PUT /api/parteien (API-001 Stufe 2). */
public record ParteiRequest(
        @NotBlank String bezeichnung,
        @NotBlank String adresse,
        boolean twintAktiv,
        @Nullable String twintMobilenummer,
        @Nullable List<Long> personenIds) {
}
