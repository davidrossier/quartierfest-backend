package ch.quartierfest.backend.benutzer;

// UC-015: Benutzer verwalten (AUTH-002)

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {

    /** API-001 Stufe 2 / PERF-001: Partei per Fetch-Join — eine Query statt 1+N bei GET /api/benutzer. */
    @Override
    @Query("select b from Benutzer b left join fetch b.partei")
    List<Benutzer> findAll();

    Optional<Benutzer> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRolle(Benutzer.Rolle rolle);
}
