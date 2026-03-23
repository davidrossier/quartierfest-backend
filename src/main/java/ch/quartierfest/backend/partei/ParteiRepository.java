package ch.quartierfest.backend.partei;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParteiRepository extends JpaRepository<Partei, Long> {
}
