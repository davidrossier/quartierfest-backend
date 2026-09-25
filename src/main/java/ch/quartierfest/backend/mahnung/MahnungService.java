package ch.quartierfest.backend.mahnung;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.abrechnung.AbrechnungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MahnungService {

    private final MahnungRepository mahnungRepository;
    private final AbrechnungRepository abrechnungRepository;

    @Transactional(readOnly = true)
    public List<MahnungResponse> findAll() {
        return mahnungRepository.findAll().stream().map(MahnungResponse::von).toList();
    }

    @Transactional
    public MahnungResponse create(MahnungRequest request) {
        Mahnung mahnung = new Mahnung();
        mahnung.setAbrechnung(Referenzen.aufloesen(abrechnungRepository, request.abrechnungId(), "Abrechnung"));
        mahnung.setDatum(request.datum());
        mahnung.setBemerkung(request.bemerkung());
        return MahnungResponse.von(mahnungRepository.save(mahnung));
    }

    @Transactional
    public void delete(Long id) {
        mahnungRepository.deleteById(id);
        mahnungRepository.flush();
    }
}
