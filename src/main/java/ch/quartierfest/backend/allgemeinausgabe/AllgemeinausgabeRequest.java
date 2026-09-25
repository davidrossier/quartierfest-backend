package ch.quartierfest.backend.allgemeinausgabe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/** UC-007: Request für POST und PUT /api/allgemeinausgaben (API-001 Stufe 2). */
public record AllgemeinausgabeRequest(
        @NotNull Long eventId,
        @NotBlank String beschreibung,
        @Nullable String herkunft,
        @NotNull BigDecimal betrag) {
}
