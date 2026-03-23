package ch.quartierfest.backend.konsumation;

import ch.quartierfest.backend.konsumationsangebot.Konsumationsangebot;
import ch.quartierfest.backend.teilnahme.Teilnahme;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "konsumation")
public class Konsumation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Teilnahme teilnahme;

    @ManyToOne(optional = false)
    private Konsumationsangebot konsumationsangebot;

    @Column(nullable = false)
    private Integer anzahl;
}
