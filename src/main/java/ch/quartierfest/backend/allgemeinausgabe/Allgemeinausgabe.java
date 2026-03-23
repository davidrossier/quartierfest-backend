package ch.quartierfest.backend.allgemeinausgabe;

import ch.quartierfest.backend.event.Event;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "allgemeinausgabe")
public class Allgemeinausgabe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Event event;

    @Column(nullable = false)
    private String beschreibung;

    private String herkunft;

    @Column(nullable = false)
    private BigDecimal betrag;
}
