package ch.quartierfest.backend.abrechnung;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbrechnungService {

    private final AbrechnungRepository abrechnungRepository;

    public List<Abrechnung> findAll() {
        return abrechnungRepository.findAll();
    }

    public Abrechnung save(Abrechnung abrechnung) {
        return abrechnungRepository.save(abrechnung);
    }

    public void delete(Long id) {
        abrechnungRepository.deleteById(id);
    }
}
