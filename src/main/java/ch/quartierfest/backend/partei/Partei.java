package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.person.Person;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "partei")
public class Partei {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String bezeichnung;

    @NotBlank
    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private boolean twintAktiv;

    private String twintMobilenummer;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "partei_id")
    private List<Person> personen;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<Long> personenIds;
}
