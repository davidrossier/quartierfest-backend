package ch.quartierfest.backend.zahlung;

import ch.quartierfest.backend.abrechnung.Abrechnung;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "zahlung")
public class Zahlung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Abrechnung abrechnung;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zahlungskanal zahlungskanal;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false)
    private BigDecimal betrag;

    public enum Zahlungskanal {
        TWINT, UEBERWEISUNG, BAR
    }
}
