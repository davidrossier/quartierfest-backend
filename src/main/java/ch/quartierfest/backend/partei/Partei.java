package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.person.Person;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "partei")
public class Partei {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private boolean twintAktiv;

    private String twintMobilenummer;

    @OneToMany
    @JoinColumn(name = "partei_id")
    private List<Person> personen;
}
