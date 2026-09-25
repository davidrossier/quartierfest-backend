package ch.quartierfest.backend.einladung;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/** UC-004: Request für POST /api/einladungen (API-001 Stufe 2). */
public record EinladungRequest(
        @NotNull Long eventId,
        @NotNull Long parteiId,
        @NotNull Einladung.EinladungStatus status,
        @Nullable Integer anzahlPersonen,
        @Nullable Boolean hilftAufstellen,
        @Nullable Boolean hilftAufraumen,
        Einladung.@Nullable BuffetBeitrag buffetBeitrag,
        @Nullable String buffetBeitragBeschreibung,
        boolean bestaetigungVersendet) {
}
