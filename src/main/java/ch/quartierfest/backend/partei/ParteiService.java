package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.person.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParteiService {

    private final ParteiRepository parteiRepository;
    private final PersonRepository personRepository;

    public List<Partei> findAll() {
        return parteiRepository.findAll();
    }

    public Partei save(Partei partei) {
        List<Long> ids = partei.getPersonenIds();
        partei.setPersonen(ids != null ? personRepository.findAllById(ids) : new ArrayList<>());
        return parteiRepository.save(partei);
    }

    public void delete(Long id) {
        parteiRepository.deleteById(id);
    }
}
