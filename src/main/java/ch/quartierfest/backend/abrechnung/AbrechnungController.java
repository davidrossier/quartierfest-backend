package ch.quartierfest.backend.abrechnung;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/abrechnungen")
@RequiredArgsConstructor
public class AbrechnungController {

    private final AbrechnungService abrechnungService;

    @GetMapping
    public List<Abrechnung> findAll() {
        return abrechnungService.findAll();
    }

    @PostMapping
    public Abrechnung create(@RequestBody Abrechnung abrechnung) {
        return abrechnungService.save(abrechnung);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        abrechnungService.delete(id);
    }
}
