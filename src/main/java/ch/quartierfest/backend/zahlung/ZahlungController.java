package ch.quartierfest.backend.zahlung;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/zahlungen", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ZahlungController {

    private final ZahlungService zahlungService;

    @GetMapping
    public List<ZahlungResponse> findAll() {
        return zahlungService.findAll();
    }

    @PostMapping
    public ZahlungResponse create(@Valid @RequestBody ZahlungRequest request) {
        return zahlungService.create(request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        zahlungService.delete(id);
    }
}
