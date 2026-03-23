package ch.quartierfest.backend.mahnung;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MahnungRepository extends JpaRepository<Mahnung, Long> {
}
