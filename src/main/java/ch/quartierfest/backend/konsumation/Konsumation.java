package ch.quartierfest.backend.konsumation;

import ch.quartierfest.backend.konsumationsangebot.Konsumationsangebot;
import ch.quartierfest.backend.teilnahme.Teilnahme;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "konsumation")
public class Konsumation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Teilnahme teilnahme;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Konsumationsangebot konsumationsangebot;

    @NotNull
    @Column(nullable = false)
    private Integer anzahl;
}
