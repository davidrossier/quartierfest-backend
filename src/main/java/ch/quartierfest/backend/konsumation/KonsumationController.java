package ch.quartierfest.backend.konsumation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/konsumationen")
@RequiredArgsConstructor
public class KonsumationController {

    private final KonsumationService konsumationService;

    @GetMapping
    public List<Konsumation> findAll() {
        return konsumationService.findAll();
    }

    @PostMapping
    public Konsumation create(@Valid @RequestBody Konsumation konsumation) {
        return konsumationService.save(konsumation);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        konsumationService.delete(id);
    }
}
