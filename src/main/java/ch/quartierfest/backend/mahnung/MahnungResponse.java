package ch.quartierfest.backend.mahnung;

import ch.quartierfest.backend.abrechnung.AbrechnungKurz;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/** UC-013: Mahnung in Antworten (API-001 Stufe 2). */
public record MahnungResponse(
        Long id,
        AbrechnungKurz abrechnung,
        LocalDate datum,
        @Nullable String bemerkung) {

    public static MahnungResponse von(Mahnung mahnung) {
        return new MahnungResponse(mahnung.getId(), AbrechnungKurz.von(mahnung.getAbrechnung()),
                mahnung.getDatum(), mahnung.getBemerkung());
    }
}
