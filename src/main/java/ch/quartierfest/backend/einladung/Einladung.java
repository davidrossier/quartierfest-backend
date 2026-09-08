package ch.quartierfest.backend.einladung;

import ch.quartierfest.backend.event.Event;
import ch.quartierfest.backend.partei.Partei;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
// DB-002: eine Einladung pro Event und Partei (UC-004 E1) — DB-Constraint uk_einladung_event_partei (V2)
@Table(name = "einladung", uniqueConstraints = @UniqueConstraint(name = "uk_einladung_event_partei", columnNames = {"event_id", "partei_id"}))
public class Einladung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
