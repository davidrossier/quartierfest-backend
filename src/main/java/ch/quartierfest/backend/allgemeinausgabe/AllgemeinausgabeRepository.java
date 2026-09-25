package ch.quartierfest.backend.allgemeinausgabe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllgemeinausgabeRepository extends JpaRepository<Allgemeinausgabe, Long> {

    /** API-001 Stufe 2 / PERF-001: Referenzen der Response per Fetch-Join — eine Query statt 1+N (TC-054). */
    @Override
    @Query("select a from Allgemeinausgabe a join fetch a.event")
    List<Allgemeinausgabe> findAll();
}
