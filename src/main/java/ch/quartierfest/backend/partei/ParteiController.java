package ch.quartierfest.backend.partei;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/parteien", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ParteiController {

    private final ParteiService parteiService;

    @GetMapping
    public List<ParteiResponse> findAll() {
        return parteiService.findAll();
    }

    @PostMapping
    public ParteiResponse create(@Valid @RequestBody ParteiRequest request) {
        return parteiService.create(request);
    }

    @PutMapping("/{id}")
    public ParteiResponse update(@PathVariable Long id, @Valid @RequestBody ParteiRequest request) {
        return parteiService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        parteiService.delete(id);
    }
}
