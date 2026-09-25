package ch.quartierfest.backend.benutzer;

// UC-015: Benutzer verwalten (AUTH-002, Eigenbau-Login)

import ch.quartierfest.backend.partei.Partei;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "benutzer", uniqueConstraints = @UniqueConstraint(name = "uk_benutzer_email", columnNames = "email"))
public class Benutzer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String email;

    // Nie ausgeliefert: BenutzerResponse kennt das Feld nicht (API-001 Stufe 2)
    @Column(nullable = false)
    private String passwortHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rolle rolle;

    @ManyToOne
    private Partei partei;

    public enum Rolle {
        ORGANISATOR, PARTEI
    }
}
