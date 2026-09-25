package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.person.Person;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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

    @OneToMany
    @JoinColumn(name = "partei_id")
    private List<Person> personen = new ArrayList<>();
}
