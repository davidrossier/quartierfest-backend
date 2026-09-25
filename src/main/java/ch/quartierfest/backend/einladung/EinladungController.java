package ch.quartierfest.backend.einladung;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/einladungen", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EinladungController {

    private final EinladungService einladungService;

    @GetMapping
    public List<EinladungResponse> findAll() {
        return einladungService.findAll();
    }

    @PostMapping
    public EinladungResponse create(@Valid @RequestBody EinladungRequest request) {
        return einladungService.create(request);
    }

    /** UC-004/UC-006: Rückmeldung und Bestätigung (REST-003) — ersetzt den früheren POST-Upsert. */
    @PutMapping("/{id}")
    public EinladungResponse update(@PathVariable Long id, @Valid @RequestBody EinladungUpdateRequest request) {
        return einladungService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        einladungService.delete(id);
    }
}
