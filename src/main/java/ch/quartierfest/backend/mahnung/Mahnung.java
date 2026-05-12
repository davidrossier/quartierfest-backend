package ch.quartierfest.backend.mahnung;

import ch.quartierfest.backend.abrechnung.Abrechnung;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "mahnung")
public class Mahnung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private Abrechnung abrechnung;

    @NotNull
    @Column(nullable = false)
    private LocalDate datum;

    private String bemerkung;
}
