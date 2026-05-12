package ch.quartierfest.backend.allgemeinausgabe;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/allgemeinausgaben")
@RequiredArgsConstructor
public class AllgemeinausgabeController {

    private final AllgemeinausgabeService allgemeinausgabeService;

    @GetMapping
    public List<Allgemeinausgabe> findAll() {
        return allgemeinausgabeService.findAll();
    }

    @PostMapping
    public Allgemeinausgabe create(@Valid @RequestBody Allgemeinausgabe allgemeinausgabe) {
        return allgemeinausgabeService.save(allgemeinausgabe);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        allgemeinausgabeService.delete(id);
    }
}
