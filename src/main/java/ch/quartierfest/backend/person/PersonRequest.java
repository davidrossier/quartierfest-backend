package ch.quartierfest.backend.person;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

/** UC-001: Request für POST und PUT /api/persons (API-001 Stufe 2). */
public record PersonRequest(
        @NotBlank String vorname,
        @NotBlank String name,
        @Nullable String telefonnummer,
        @Nullable String mobilenummer,
        @Nullable String email) {
}
