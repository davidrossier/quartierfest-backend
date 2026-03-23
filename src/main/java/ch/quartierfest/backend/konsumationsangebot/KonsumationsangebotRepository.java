package ch.quartierfest.backend.konsumationsangebot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KonsumationsangebotRepository extends JpaRepository<Konsumationsangebot, Long> {
}
