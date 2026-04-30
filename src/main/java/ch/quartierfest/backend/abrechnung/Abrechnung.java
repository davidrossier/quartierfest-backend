package ch.quartierfest.backend.abrechnung;

import ch.quartierfest.backend.teilnahme.Teilnahme;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "abrechnung")
public class Abrechnung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO [UC-011]: Kein @UniqueConstraint auf teilnahme_id — doppelte Abrechnung möglich wenn UI-Logik umgangen wird – siehe specs/UC-011_Abrechnung-Erstellen.md
    @OneToOne(optional = false)
    private Teilnahme teilnahme;

    @Column(nullable = false)
    private BigDecimal anteilAllgemeinkosten;

    @Column(nullable = false)
    private BigDecimal totalKonsumation;

    @Column(nullable = false)
    private BigDecimal totalBetrag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zustellungskanal zustellungskanal;

    private LocalDate zustellungsDatum;

    public enum Zustellungskanal {
        TWINT, EMAIL, PAPIER
    }
}
