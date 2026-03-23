package ch.quartierfest.backend.abrechnung;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbrechnungRepository extends JpaRepository<Abrechnung, Long> {
}
