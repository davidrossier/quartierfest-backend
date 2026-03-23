package ch.quartierfest.backend.einladung;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/einladungen")
@RequiredArgsConstructor
public class EinladungController {

    private final EinladungService einladungService;

    @GetMapping
    public List<Einladung> findAll() {
        return einladungService.findAll();
    }

    @PostMapping
    public Einladung create(@RequestBody Einladung einladung) {
        return einladungService.save(einladung);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        einladungService.delete(id);
    }
}
