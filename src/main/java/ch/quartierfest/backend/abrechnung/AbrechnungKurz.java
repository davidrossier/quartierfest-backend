package ch.quartierfest.backend.abrechnung;

/** Abrechnung als eingebettete Referenz (Zahlung, Mahnung) — das Frontend liest dort nur die id (API-001 Stufe 2). */
public record AbrechnungKurz(Long id) {

    public static AbrechnungKurz von(Abrechnung abrechnung) {
        return new AbrechnungKurz(abrechnung.getId());
    }
}
