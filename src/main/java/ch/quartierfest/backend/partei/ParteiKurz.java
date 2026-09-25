package ch.quartierfest.backend.partei;

import org.jspecify.annotations.Nullable;

/**
 * Partei als eingebettete Referenz (Einladung, Teilnahme, Abrechnung, Benutzer) — bewusst ohne
 * {@code personen}, damit verschachtelte Antworten keine Collections nachladen (API-001 Stufe 2, PERF-001).
 */
public record ParteiKurz(
        Long id,
        String bezeichnung,
        String adresse,
        boolean twintAktiv,
        @Nullable String twintMobilenummer) {

    public static ParteiKurz von(Partei partei) {
        return new ParteiKurz(partei.getId(), partei.getBezeichnung(), partei.getAdresse(),
                partei.isTwintAktiv(), partei.getTwintMobilenummer());
    }
}
