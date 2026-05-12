package ch.quartierfest.backend.event;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate datum;

    @NotNull
    @Column(nullable = false)
    private LocalTime startzeit;

    @NotBlank
    @Column(nullable = false)
    private String standort;

    private String alternativerStandort;

    private LocalTime zeitAufstellen;

    private LocalTime zeitAufraumen;
}
