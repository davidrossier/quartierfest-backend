package ch.quartierfest.backend.mahnung;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mahnungen")
@RequiredArgsConstructor
public class MahnungController {

    private final MahnungService mahnungService;

    @GetMapping
    public List<Mahnung> findAll() {
        return mahnungService.findAll();
    }

    @PostMapping
    public Mahnung create(@RequestBody Mahnung mahnung) {
        return mahnungService.save(mahnung);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mahnungService.delete(id);
    }
}
