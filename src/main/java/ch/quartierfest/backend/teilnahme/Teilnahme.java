package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.Einladung.BuffetBeitrag;
import ch.quartierfest.backend.einladung.Einladung;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "teilnahme")
public class Teilnahme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    private Einladung einladung;

    private Integer anzahlPersonenEffektiv;

    private Boolean hilftAufstellen;

    private Boolean hilftAufraumen;

    @Enumerated(EnumType.STRING)
    private BuffetBeitrag buffetBeitrag;

    private String buffetBeitragBeschreibung;
}
