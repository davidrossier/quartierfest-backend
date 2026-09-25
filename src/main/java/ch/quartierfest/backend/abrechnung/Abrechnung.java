package ch.quartierfest.backend.abrechnung;

import ch.quartierfest.backend.teilnahme.Teilnahme;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
// DB-002: Teilnahme 1—1 Abrechnung — DB-Constraint uk_abrechnung_teilnahme (V2)
@Table(name = "abrechnung", uniqueConstraints = @UniqueConstraint(name = "uk_abrechnung_teilnahme", columnNames = "teilnahme_id"))
public class Abrechnung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(optional = false)
    private Teilnahme teilnahme;

    // DB-002: Geldbeträge explizit numeric(10,2)
    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal anteilAllgemeinkosten;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalKonsumation;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalBetrag;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zustellungskanal zustellungskanal;

    private LocalDate zustellungsDatum;

    public enum Zustellungskanal {
        TWINT, EMAIL, PAPIER
    }
}
