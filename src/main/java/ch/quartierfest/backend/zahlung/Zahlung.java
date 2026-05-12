package ch.quartierfest.backend.zahlung;

import ch.quartierfest.backend.abrechnung.Abrechnung;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    @ManyToOne(optional = false)
    private Abrechnung abrechnung;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zahlungskanal zahlungskanal;

    @NotNull
    @Column(nullable = false)
    private LocalDate datum;

    @NotNull
    @Column(nullable = false)
    private BigDecimal betrag;

    public enum Zahlungskanal {
        TWINT, UEBERWEISUNG, BAR
    }
}
