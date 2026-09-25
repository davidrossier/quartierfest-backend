package ch.quartierfest.backend.konsumation;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.konsumationsangebot.KonsumationsangebotRepository;
import ch.quartierfest.backend.teilnahme.TeilnahmeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KonsumationService {

    private final KonsumationRepository konsumationRepository;
    private final TeilnahmeRepository teilnahmeRepository;
    private final KonsumationsangebotRepository konsumationsangebotRepository;

    @Transactional(readOnly = true)
    public List<KonsumationResponse> findAll() {
        return konsumationRepository.findAll().stream().map(KonsumationResponse::von).toList();
    }

    @Transactional
    public KonsumationResponse create(KonsumationRequest request) {
        Konsumation konsumation = new Konsumation();
        konsumation.setTeilnahme(Referenzen.aufloesen(teilnahmeRepository, request.teilnahmeId(), "Teilnahme"));
        konsumation.setKonsumationsangebot(Referenzen.aufloesen(konsumationsangebotRepository,
                request.konsumationsangebotId(), "Konsumationsangebot"));
        konsumation.setAnzahl(request.anzahl());
        return KonsumationResponse.von(konsumationRepository.save(konsumation));
    }

    /** UC-010: Anzahl einer bestehenden Matrix-Zelle ändern (REST-003, erweitert) — unbekannte id → 404. */
    @Transactional
    public KonsumationResponse update(Long id, KonsumationUpdateRequest request) {
        Konsumation konsumation = Referenzen.laden(konsumationRepository, id, "Konsumation");
        konsumation.setAnzahl(request.anzahl());
        return KonsumationResponse.von(konsumationRepository.saveAndFlush(konsumation));
    }

    @Transactional
    public void delete(Long id) {
        konsumationRepository.deleteById(id);
        konsumationRepository.flush();
    }
}
