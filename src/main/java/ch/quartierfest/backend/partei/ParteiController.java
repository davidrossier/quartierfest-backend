package ch.quartierfest.backend.partei;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/parteien")
@RequiredArgsConstructor
public class ParteiController {

    private final ParteiService parteiService;

    @GetMapping
    public List<Partei> findAll() {
        return parteiService.findAll();
    }

    @PostMapping
    public Partei create(@RequestBody Partei partei) {
        return parteiService.save(partei);
    }

    @PutMapping("/{id}")
    public Partei update(@PathVariable Long id, @RequestBody Partei partei) {
        partei.setId(id);
        return parteiService.save(partei);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        parteiService.delete(id);
    }
}
