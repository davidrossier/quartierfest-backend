package ch.quartierfest.backend.einladung;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EinladungRepository extends JpaRepository<Einladung, Long> {

    /** API-001 Stufe 2 / PERF-001: Referenzen der Response per Fetch-Join — eine Query statt 1+N (TC-054). */
    @Override
    @Query("select e from Einladung e join fetch e.event join fetch e.partei")
    List<Einladung> findAll();
}
