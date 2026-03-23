package ch.quartierfest.backend.zahlung;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/zahlungen")
@RequiredArgsConstructor
public class ZahlungController {

    private final ZahlungService zahlungService;

    @GetMapping
    public List<Zahlung> findAll() {
        return zahlungService.findAll();
    }

    @PostMapping
    public Zahlung create(@RequestBody Zahlung zahlung) {
        return zahlungService.save(zahlung);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        zahlungService.delete(id);
    }
}
