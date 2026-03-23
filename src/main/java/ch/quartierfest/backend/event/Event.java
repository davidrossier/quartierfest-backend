package ch.quartierfest.backend.event;

import jakarta.persistence.*;
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

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false)
    private LocalTime startzeit;

    @Column(nullable = false)
    private String standort;

    private String alternativerStandort;

    private LocalTime zeitAufstellen;

    private LocalTime zeitAufraumen;
}
