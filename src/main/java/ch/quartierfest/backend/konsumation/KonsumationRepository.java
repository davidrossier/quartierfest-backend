package ch.quartierfest.backend.konsumation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KonsumationRepository extends JpaRepository<Konsumation, Long> {

    /** API-001 Stufe 2 / PERF-001: Referenzen der Response per Fetch-Join — eine Query statt 1+N (TC-054). */
    @Override
    @Query("select k from Konsumation k join fetch k.teilnahme t join fetch t.einladung e join fetch e.event join fetch e.partei join fetch k.konsumationsangebot")
    List<Konsumation> findAll();
}
