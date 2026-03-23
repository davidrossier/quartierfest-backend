package ch.quartierfest.backend.mahnung;

import ch.quartierfest.backend.abrechnung.Abrechnung;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "mahnung")
public class Mahnung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Abrechnung abrechnung;

    @Column(nullable = false)
    private LocalDate datum;

    private String bemerkung;
}
