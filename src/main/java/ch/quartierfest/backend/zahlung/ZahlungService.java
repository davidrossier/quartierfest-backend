package ch.quartierfest.backend.zahlung;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ZahlungService {

    private final ZahlungRepository zahlungRepository;

    public List<Zahlung> findAll() {
        return zahlungRepository.findAll();
    }

    public Zahlung save(Zahlung zahlung) {
        return zahlungRepository.save(zahlung);
    }

    public void delete(Long id) {
        zahlungRepository.deleteById(id);
    }
}
