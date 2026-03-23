package ch.quartierfest.backend.teilnahme;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeilnahmeRepository extends JpaRepository<Teilnahme, Long> {
}
