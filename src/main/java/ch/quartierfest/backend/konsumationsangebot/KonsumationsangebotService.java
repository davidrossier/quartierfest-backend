package ch.quartierfest.backend.konsumationsangebot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KonsumationsangebotService {

    private final KonsumationsangebotRepository konsumationsangebotRepository;

    public List<Konsumationsangebot> findAll() {
        return konsumationsangebotRepository.findAll();
    }

    public Konsumationsangebot save(Konsumationsangebot konsumationsangebot) {
        return konsumationsangebotRepository.save(konsumationsangebot);
    }

    public void delete(Long id) {
        konsumationsangebotRepository.deleteById(id);
    }
}
