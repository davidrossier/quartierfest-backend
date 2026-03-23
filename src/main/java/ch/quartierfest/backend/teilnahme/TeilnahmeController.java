package ch.quartierfest.backend.teilnahme;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/teilnahmen")
@RequiredArgsConstructor
public class TeilnahmeController {

    private final TeilnahmeService teilnahmeService;

    @GetMapping
    public List<Teilnahme> findAll() {
        return teilnahmeService.findAll();
    }

    @PostMapping
    public Teilnahme create(@RequestBody Teilnahme teilnahme) {
        return teilnahmeService.save(teilnahme);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        teilnahmeService.delete(id);
    }
}
