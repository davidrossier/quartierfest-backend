package ch.quartierfest.backend.konsumation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KonsumationService {

    private final KonsumationRepository konsumationRepository;

    public List<Konsumation> findAll() {
        return konsumationRepository.findAll();
    }

    public Konsumation save(Konsumation konsumation) {
        return konsumationRepository.save(konsumation);
    }

    public void delete(Long id) {
        konsumationRepository.deleteById(id);
    }
}
