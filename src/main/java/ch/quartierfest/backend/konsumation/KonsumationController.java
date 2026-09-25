package ch.quartierfest.backend.konsumation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/konsumationen", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class KonsumationController {

    private final KonsumationService konsumationService;

    @GetMapping
    public List<KonsumationResponse> findAll() {
        return konsumationService.findAll();
    }

    @PostMapping
    public KonsumationResponse create(@Valid @RequestBody KonsumationRequest request) {
        return konsumationService.create(request);
    }

    @PutMapping("/{id}")
    public KonsumationResponse update(@PathVariable Long id, @Valid @RequestBody KonsumationUpdateRequest request) {
        return konsumationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        konsumationService.delete(id);
    }
}
