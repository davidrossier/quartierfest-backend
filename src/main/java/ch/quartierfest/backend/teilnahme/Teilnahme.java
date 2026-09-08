package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.Einladung;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
// DB-002: Einladung 1—1 Teilnahme — DB-Constraint uk_teilnahme_einladung (V2)
@Table(name = "teilnahme", uniqueConstraints = @UniqueConstraint(name = "uk_teilnahme_einladung", columnNames = "einladung_id"))
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
