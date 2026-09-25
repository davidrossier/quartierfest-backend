package ch.quartierfest.backend.abrechnung;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.teilnahme.TeilnahmeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AbrechnungService {

    private final AbrechnungRepository abrechnungRepository;
    private final TeilnahmeRepository teilnahmeRepository;

    @Transactional(readOnly = true)
    public List<AbrechnungResponse> findAll() {
        return abrechnungRepository.findAll().stream().map(AbrechnungResponse::von).toList();
    }

    @Transactional
    public AbrechnungResponse create(AbrechnungRequest request) {
        Abrechnung abrechnung = new Abrechnung();
        abrechnung.setTeilnahme(Referenzen.aufloesen(teilnahmeRepository, request.teilnahmeId(), "Teilnahme"));
        abrechnung.setAnteilAllgemeinkosten(request.anteilAllgemeinkosten());
        abrechnung.setTotalKonsumation(request.totalKonsumation());
        abrechnung.setTotalBetrag(request.totalBetrag());
        abrechnung.setZustellungskanal(request.zustellungskanal());
        abrechnung.setZustellungsDatum(request.zustellungsDatum());
        return AbrechnungResponse.von(abrechnungRepository.save(abrechnung));
    }

    /** UC-011/UC-012: Kanal, Zustelldatum, Beträge ändern (REST-003) — unbekannte id → 404. */
    @Transactional
    public AbrechnungResponse update(Long id, AbrechnungUpdateRequest request) {
        Abrechnung abrechnung = Referenzen.laden(abrechnungRepository, id, "Abrechnung");
        abrechnung.setAnteilAllgemeinkosten(request.anteilAllgemeinkosten());
        abrechnung.setTotalKonsumation(request.totalKonsumation());
        abrechnung.setTotalBetrag(request.totalBetrag());
        abrechnung.setZustellungskanal(request.zustellungskanal());
        abrechnung.setZustellungsDatum(request.zustellungsDatum());
        return AbrechnungResponse.von(abrechnungRepository.saveAndFlush(abrechnung));
    }

    @Transactional
    public void delete(Long id) {
        abrechnungRepository.deleteById(id);
        abrechnungRepository.flush();
    }
}
