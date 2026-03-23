package ch.quartierfest.backend.allgemeinausgabe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllgemeinausgabeRepository extends JpaRepository<Allgemeinausgabe, Long> {
}
