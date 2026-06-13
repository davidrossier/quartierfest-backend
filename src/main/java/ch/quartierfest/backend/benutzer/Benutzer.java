package ch.quartierfest.backend.benutzer;

// UC-015: Benutzer verwalten (AUTH-002, Eigenbau-Login)

import ch.quartierfest.backend.partei.Partei;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "benutzer")
public class Benutzer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String passwortHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rolle rolle;

    @ManyToOne
    private Partei partei;

    // Nur beim POST entgegengenommen; wird im Service gehasht und nie ausgeliefert
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank
    @Size(min = 10)
    private String passwort;

    public enum Rolle {
        ORGANISATOR, PARTEI
    }
}
