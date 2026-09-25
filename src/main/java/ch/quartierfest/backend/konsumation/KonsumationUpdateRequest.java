package ch.quartierfest.backend.konsumation;

import jakarta.validation.constraints.NotNull;

/**
 * UC-010: Whitelist für PUT /api/konsumationen/{id} (REST-003, erweitert) — in der Konsumationsmatrix
 * ändert sich nur die Anzahl; Teilnahme und Angebot einer Zelle sind fix.
 */
public record KonsumationUpdateRequest(
        @NotNull Integer anzahl) {
}
