package ch.quartierfest.backend.zahlung;

import ch.quartierfest.backend.abrechnung.Abrechnung;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "zahlung")
public class Zahlung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Abrechnung abrechnung;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zahlungskanal zahlungskanal;

    @NotNull
    @Column(nullable = false)
    private LocalDate datum;

    // DB-002: Geldbetrag explizit numeric(10,2)
    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal betrag;

    public enum Zahlungskanal {
        TWINT, UEBERWEISUNG, BAR
    }
}
