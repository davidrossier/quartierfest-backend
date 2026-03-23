package ch.quartierfest.backend.allgemeinausgabe;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AllgemeinausgabeService {

    private final AllgemeinausgabeRepository allgemeinausgabeRepository;

    public List<Allgemeinausgabe> findAll() {
        return allgemeinausgabeRepository.findAll();
    }

    public Allgemeinausgabe save(Allgemeinausgabe allgemeinausgabe) {
        return allgemeinausgabeRepository.save(allgemeinausgabe);
    }

    public void delete(Long id) {
        allgemeinausgabeRepository.deleteById(id);
    }
}
