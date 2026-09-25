package ch.quartierfest.backend.zahlung;

import ch.quartierfest.backend.abrechnung.AbrechnungKurz;

import java.math.BigDecimal;
import java.time.LocalDate;

/** UC-013: Zahlung in Antworten (API-001 Stufe 2). */
public record ZahlungResponse(
        Long id,
        AbrechnungKurz abrechnung,
        Zahlung.Zahlungskanal zahlungskanal,
        LocalDate datum,
        BigDecimal betrag) {

    public static ZahlungResponse von(Zahlung zahlung) {
        return new ZahlungResponse(zahlung.getId(), AbrechnungKurz.von(zahlung.getAbrechnung()),
                zahlung.getZahlungskanal(), zahlung.getDatum(), zahlung.getBetrag());
    }
}
