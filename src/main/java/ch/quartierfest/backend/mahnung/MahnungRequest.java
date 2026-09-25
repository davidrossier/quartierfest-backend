package ch.quartierfest.backend.mahnung;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/** UC-013: Request für POST /api/mahnungen (API-001 Stufe 2). */
public record MahnungRequest(
        @NotNull Long abrechnungId,
        @NotNull LocalDate datum,
        @Nullable String bemerkung) {
}
