package ch.quartierfest.backend.einladung;

import ch.quartierfest.backend.event.Event;
import ch.quartierfest.backend.partei.Partei;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "einladung")
public class Einladung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO [UC-004]: Kein @UniqueConstraint auf (event_id, partei_id) — Duplikat-Einladungen möglich – siehe specs/UC-004_Einladung-Verwalten.md
    @NotNull
    @ManyToOne(optional = false)
    private Event event;

    @NotNull
    @ManyToOne(optional = false)
    private Partei partei;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EinladungStatus status;

    private Integer anzahlPersonen;

    private Boolean hilftAufstellen;

    private Boolean hilftAufraumen;

    @Enumerated(EnumType.STRING)
    private BuffetBeitrag buffetBeitrag;

    private String buffetBeitragBeschreibung;

    @Column(nullable = false)
    private boolean bestaetigungVersendet;

    public enum EinladungStatus {
        OFFEN, ANGEMELDET, ABGEMELDET
    }

    public enum BuffetBeitrag {
        KEINER, SALAT, BROT_ZOPF, DESSERT, WEITERE
    }
}
