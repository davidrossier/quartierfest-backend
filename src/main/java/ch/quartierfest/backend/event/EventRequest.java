package ch.quartierfest.backend.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;

/** UC-003: Request für POST und PUT /api/events (API-001 Stufe 2). */
public record EventRequest(
        @NotNull LocalDate datum,
        @NotNull LocalTime startzeit,
        @NotBlank String standort,
        @Nullable String alternativerStandort,
        @Nullable LocalTime zeitAufstellen,
        @Nullable LocalTime zeitAufraumen) {
}
