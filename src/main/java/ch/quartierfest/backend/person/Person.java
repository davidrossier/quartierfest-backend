package ch.quartierfest.backend.person;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "person")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String vorname;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String telefonnummer;

    private String mobilenummer;

    private String email;
}