package ch.quartierfest.backend.allgemeinausgabe;

import ch.quartierfest.backend.event.Event;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "allgemeinausgabe")
public class Allgemeinausgabe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private Event event;

    @NotBlank
    @Column(nullable = false)
    private String beschreibung;

    private String herkunft;

    // DB-002: Geldbetrag explizit numeric(10,2)
    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal betrag;
}
