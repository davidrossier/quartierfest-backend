package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.EinladungKurz;

/**
 * Teilnahme als eingebettete Referenz (Konsumation, Abrechnung) — ohne Buffet-Beiträge, nur was das
 * Frontend über {@code …teilnahme.id} und {@code …teilnahme.einladung.*} liest (API-001 Stufe 2, E3).
 */
public record TeilnahmeKurz(
        Long id,
        EinladungKurz einladung) {

    public static TeilnahmeKurz von(Teilnahme teilnahme) {
        return new TeilnahmeKurz(teilnahme.getId(), EinladungKurz.von(teilnahme.getEinladung()));
    }
}
