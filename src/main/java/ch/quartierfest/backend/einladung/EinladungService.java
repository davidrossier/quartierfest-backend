package ch.quartierfest.backend.einladung;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EinladungService {

    private final EinladungRepository einladungRepository;

    public List<Einladung> findAll() {
        return einladungRepository.findAll();
    }

    public Einladung save(Einladung einladung) {
        return einladungRepository.save(einladung);
    }

    public void delete(Long id) {
        einladungRepository.deleteById(id);
    }
}
