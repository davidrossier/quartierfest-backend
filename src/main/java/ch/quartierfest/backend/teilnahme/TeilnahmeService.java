package ch.quartierfest.backend.teilnahme;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeilnahmeService {

    private final TeilnahmeRepository teilnahmeRepository;

    public List<Teilnahme> findAll() {
        return teilnahmeRepository.findAll();
    }

    public Teilnahme save(Teilnahme teilnahme) {
        return teilnahmeRepository.save(teilnahme);
    }

    public void delete(Long id) {
        teilnahmeRepository.deleteById(id);
    }
}
