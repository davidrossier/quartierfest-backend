package ch.quartierfest.backend.mahnung;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MahnungService {

    private final MahnungRepository mahnungRepository;

    public List<Mahnung> findAll() {
        return mahnungRepository.findAll();
    }

    public Mahnung save(Mahnung mahnung) {
        return mahnungRepository.save(mahnung);
    }

    public void delete(Long id) {
        mahnungRepository.deleteById(id);
    }
}
