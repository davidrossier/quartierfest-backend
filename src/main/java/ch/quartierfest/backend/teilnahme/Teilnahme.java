package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.Einladung;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "teilnahme")
public class Teilnahme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(optional = false)
    private Einladung einladung;

    private Integer anzahlPersonenEffektiv;

    private Boolean hilftAufstellen;

    private Boolean hilftAufraumen;

    @ElementCollection
    @CollectionTable(name = "teilnahme_buffet_beitrag", joinColumns = @JoinColumn(name = "teilnahme_id"))
    private List<TeilnahmeBuffetBeitrag> buffetBeitraege = new ArrayList<>();
}
