package ch.quartierfest.backend.konsumationsangebot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KonsumationsangebotRepository extends JpaRepository<Konsumationsangebot, Long> {

    /** API-001 Stufe 2 / PERF-001: Referenzen der Response per Fetch-Join — eine Query statt 1+N (TC-054). */
    @Override
    @Query("select k from Konsumationsangebot k join fetch k.event")
    List<Konsumationsangebot> findAll();
}
