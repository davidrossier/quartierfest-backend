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

    @NotNull
    @Column(nullable = false)
    private BigDecimal betrag;
}
