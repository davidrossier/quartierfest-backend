package ch.quartierfest.backend.teilnahme;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TeilnahmeRepository extends JpaRepository<Teilnahme, Long> {

    /**
     * PERF-001 / API-001 Stufe 2: Buffet-Beiträge und Einladung mit Event und Partei per Fetch-Join —
     * eine Query statt 1+N bei GET /api/teilnahmen (TC-054).
     */
    @Override
    @Query("""
            select t from Teilnahme t
            join fetch t.einladung e
            join fetch e.event
            join fetch e.partei
            left join fetch t.buffetBeitraege
            """)
    List<Teilnahme> findAll();

    /**
     * UC-016: Teilnahmen einer Partei ab Stichtag, früheste zuerst —
     * das erste Element ist die Teilnahme zum «nächsten Event».
     */
    @Query("""
            select t from Teilnahme t
            join fetch t.einladung e
            join fetch e.event
            join fetch e.partei
            left join fetch t.buffetBeitraege
            where e.partei.id = :parteiId
              and e.event.datum >= :stichtag
            order by e.event.datum asc
            """)
    List<Teilnahme> findEigeneAbStichtag(@Param("parteiId") Long parteiId,
                                         @Param("stichtag") LocalDate stichtag);

    /** UC-016: Ownership-Prüfung für TeilnahmeZugriff — gehört die Teilnahme zur Partei? */
    boolean existsByIdAndEinladungParteiId(Long id, Long parteiId);
}
