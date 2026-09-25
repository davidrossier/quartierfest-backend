package ch.quartierfest.backend.benutzer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * UC-015: Request für POST /api/benutzer (API-001 Stufe 2). Das Klartext-Passwort existiert nur hier;
 * der Service speichert ausschliesslich den Hash.
 */
public record BenutzerRequest(
        @NotBlank String email,
        @NotBlank @Size(min = 10) String passwort,
        @NotNull Benutzer.Rolle rolle,
        @Nullable Long parteiId) {
}
