package ch.quartierfest.backend.abrechnung;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbrechnungRepository extends JpaRepository<Abrechnung, Long> {

    /** API-001 Stufe 2 / PERF-001: Referenzen der Response per Fetch-Join — eine Query statt 1+N (TC-054). */
    @Override
    @Query("select a from Abrechnung a join fetch a.teilnahme t join fetch t.einladung e join fetch e.event join fetch e.partei")
    List<Abrechnung> findAll();
}
