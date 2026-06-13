package ch.quartierfest.backend.benutzer;

// UC-015: Benutzer verwalten (AUTH-002)

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {

    Optional<Benutzer> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRolle(Benutzer.Rolle rolle);
}
