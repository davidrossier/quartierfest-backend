package ch.quartierfest.backend.konsumationsangebot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/konsumationsangebote", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class KonsumationsangebotController {

    private final KonsumationsangebotService konsumationsangebotService;

    @GetMapping
    public List<KonsumationsangebotResponse> findAll() {
        return konsumationsangebotService.findAll();
    }

    @PostMapping
    public KonsumationsangebotResponse create(@Valid @RequestBody KonsumationsangebotRequest request) {
        return konsumationsangebotService.create(request);
    }

    @PutMapping("/{id}")
    public KonsumationsangebotResponse update(@PathVariable Long id,
                                              @Valid @RequestBody KonsumationsangebotRequest request) {
        return konsumationsangebotService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        konsumationsangebotService.delete(id);
    }
}
