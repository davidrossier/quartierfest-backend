package ch.quartierfest.backend.konsumation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KonsumationRepository extends JpaRepository<Konsumation, Long> {
}
