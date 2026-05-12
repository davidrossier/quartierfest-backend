package ch.quartierfest.backend.konsumation;

import ch.quartierfest.backend.konsumationsangebot.Konsumationsangebot;
import ch.quartierfest.backend.teilnahme.Teilnahme;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "konsumation")
public class Konsumation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private Teilnahme teilnahme;

    @NotNull
    @ManyToOne(optional = false)
    private Konsumationsangebot konsumationsangebot;

    @NotNull
    @Column(nullable = false)
    private Integer anzahl;
}
