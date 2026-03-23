package ch.quartierfest.backend.zahlung;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZahlungRepository extends JpaRepository<Zahlung, Long> {
}
