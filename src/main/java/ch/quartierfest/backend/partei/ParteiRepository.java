package ch.quartierfest.backend.partei;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParteiRepository extends JpaRepository<Partei, Long> {

    /** PERF-001: Personen per Fetch-Join laden — eine Query statt 1+N bei GET /api/parteien. */
    @Override
    @Query("select p from Partei p left join fetch p.personen")
    List<Partei> findAll();
}
