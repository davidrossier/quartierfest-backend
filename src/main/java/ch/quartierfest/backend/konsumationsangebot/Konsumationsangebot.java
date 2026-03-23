package ch.quartierfest.backend.konsumationsangebot;

import ch.quartierfest.backend.event.Event;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "konsumationsangebot")
public class Konsumationsangebot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Event event;

    @Column(nullable = false)
    private String bezeichnung;

    @Column(nullable = false)
    private BigDecimal preis;
}
