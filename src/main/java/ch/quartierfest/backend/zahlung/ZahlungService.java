package ch.quartierfest.backend.zahlung;

import ch.quartierfest.backend.Referenzen;
import ch.quartierfest.backend.abrechnung.AbrechnungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZahlungService {

    private final ZahlungRepository zahlungRepository;
    private final AbrechnungRepository abrechnungRepository;

    @Transactional(readOnly = true)
    public List<ZahlungResponse> findAll() {
        return zahlungRepository.findAll().stream().map(ZahlungResponse::von).toList();
    }

    @Transactional
    public ZahlungResponse create(ZahlungRequest request) {
        Zahlung zahlung = new Zahlung();
        zahlung.setAbrechnung(Referenzen.aufloesen(abrechnungRepository, request.abrechnungId(), "Abrechnung"));
        zahlung.setZahlungskanal(request.zahlungskanal());
        zahlung.setDatum(request.datum());
        zahlung.setBetrag(request.betrag());
        return ZahlungResponse.von(zahlungRepository.save(zahlung));
    }

    @Transactional
    public void delete(Long id) {
        zahlungRepository.deleteById(id);
        zahlungRepository.flush();
    }
}
