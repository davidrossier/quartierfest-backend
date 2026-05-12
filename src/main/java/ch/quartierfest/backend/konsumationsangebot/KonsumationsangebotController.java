package ch.quartierfest.backend.konsumationsangebot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/konsumationsangebote")
@RequiredArgsConstructor
public class KonsumationsangebotController {

    private final KonsumationsangebotService konsumationsangebotService;

    @GetMapping
    public List<Konsumationsangebot> findAll() {
        return konsumationsangebotService.findAll();
    }

    @PostMapping
    public Konsumationsangebot create(@Valid @RequestBody Konsumationsangebot konsumationsangebot) {
        return konsumationsangebotService.save(konsumationsangebot);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        konsumationsangebotService.delete(id);
    }
}
