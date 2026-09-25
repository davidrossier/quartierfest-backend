package ch.quartierfest.backend.abrechnung;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/abrechnungen", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AbrechnungController {

    private final AbrechnungService abrechnungService;

    @GetMapping
    public List<AbrechnungResponse> findAll() {
        return abrechnungService.findAll();
    }

    @PostMapping
    public AbrechnungResponse create(@Valid @RequestBody AbrechnungRequest request) {
        return abrechnungService.create(request);
    }

    /** UC-011/UC-012: ersetzt den früheren POST-Upsert (REST-003). */
    @PutMapping("/{id}")
    public AbrechnungResponse update(@PathVariable Long id, @Valid @RequestBody AbrechnungUpdateRequest request) {
        return abrechnungService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        abrechnungService.delete(id);
    }
}
