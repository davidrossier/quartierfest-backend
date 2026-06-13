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
     * UC-016: Teilnahmen einer Partei ab Stichtag, früheste zuerst —
     * das erste Element ist die Teilnahme zum «nächsten Event».
     */
    @Query("""
            select t from Teilnahme t
            where t.einladung.partei.id = :parteiId
              and t.einladung.event.datum >= :stichtag
            order by t.einladung.event.datum asc
            """)
    List<Teilnahme> findEigeneAbStichtag(@Param("parteiId") Long parteiId,
                                         @Param("stichtag") LocalDate stichtag);
}
