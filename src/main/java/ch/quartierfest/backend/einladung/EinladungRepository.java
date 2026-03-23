package ch.quartierfest.backend.einladung;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EinladungRepository extends JpaRepository<Einladung, Long> {
}
