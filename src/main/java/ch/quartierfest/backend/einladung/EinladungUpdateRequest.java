package ch.quartierfest.backend.einladung;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * UC-004/UC-006: Whitelist für PUT /api/einladungen/{id} (REST-003) — Rückmeldung erfassen und
 * Bestätigung als versendet markieren. Event und Partei sind nach dem Anlegen nicht mehr änderbar;
 * {@code eventId}/{@code parteiId} im Body werden als unbekannte Felder mit 400 abgelehnt.
 */
public record EinladungUpdateRequest(
        @NotNull Einladung.EinladungStatus status,
        @Nullable Integer anzahlPersonen,
        @Nullable Boolean hilftAufstellen,
        @Nullable Boolean hilftAufraumen,
        Einladung.@Nullable BuffetBeitrag buffetBeitrag,
        @Nullable String buffetBeitragBeschreibung,
        boolean bestaetigungVersendet) {
}
