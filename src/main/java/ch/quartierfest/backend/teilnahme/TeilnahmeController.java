package ch.quartierfest.backend.teilnahme;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/teilnahmen")
@RequiredArgsConstructor
public class TeilnahmeController {

    private final TeilnahmeService teilnahmeService;

    @GetMapping
    public List<Teilnahme> findAll() {
        return teilnahmeService.findAll();
    }

    /** UC-016: Teilnahme der eigenen Partei für den nächsten Event. */
    @GetMapping("/meine")
    public Teilnahme findMeine(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Anmeldung erforderlich.");
        }
        return teilnahmeService.findMeine(jwt.getSubject());
    }

    @PostMapping
    public Teilnahme create(@Valid @RequestBody Teilnahme teilnahme) {
        return teilnahmeService.save(teilnahme);
    }

    /** UC-016: Teilnahme bestätigen/anpassen (PARTEI nur die eigene, ORGANISATOR alle). */
    @PutMapping("/{id}")
    public Teilnahme update(@PathVariable Long id, @Valid @RequestBody TeilnahmeUpdateRequest request) {
        return teilnahmeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        teilnahmeService.delete(id);
    }
}
