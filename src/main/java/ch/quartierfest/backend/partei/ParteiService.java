package ch.quartierfest.backend.partei;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParteiService {

    private final ParteiRepository parteiRepository;

    public List<Partei> findAll() {
        return parteiRepository.findAll();
    }

    public Partei save(Partei partei) {
        return parteiRepository.save(partei);
    }

    public void delete(Long id) {
        parteiRepository.deleteById(id);
    }
}
