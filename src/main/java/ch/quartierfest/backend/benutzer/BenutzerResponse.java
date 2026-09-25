package ch.quartierfest.backend.benutzer;

import ch.quartierfest.backend.partei.ParteiKurz;
import org.jspecify.annotations.Nullable;

/** UC-015: Benutzer in Antworten — nie mit Passwort oder Hash (API-001 Stufe 2). */
public record BenutzerResponse(
        Long id,
        String email,
        Benutzer.Rolle rolle,
        @Nullable ParteiKurz partei) {

    public static BenutzerResponse von(Benutzer benutzer) {
        return new BenutzerResponse(benutzer.getId(), benutzer.getEmail(), benutzer.getRolle(),
                benutzer.getPartei() == null ? null : ParteiKurz.von(benutzer.getPartei()));
    }
}
