package ch.quartierfest.backend.teilnahme;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value = "/api/teilnahmen", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TeilnahmeController {

    private final TeilnahmeService teilnahmeService;

    @GetMapping
    public List<TeilnahmeResponse> findAll() {
        return teilnahmeService.findAll();
    }

    /** UC-016: Teilnahme der eigenen Partei für den nächsten Event. */
    @GetMapping("/meine")
    public TeilnahmeResponse findMeine(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Anmeldung erforderlich.");
        }
        return teilnahmeService.findMeine(jwt.getSubject());
    }

    /**
     * UC-005: Teilnahme anlegen. REST-001: kein Upsert via POST — {@link TeilnahmeRequest} hat kein
     * id-Feld, ein POST mit id scheitert als unbekanntes Feld mit 400 (API-001 Stufe 2, E6).
     */
    @PostMapping
    public TeilnahmeResponse create(@Valid @RequestBody TeilnahmeRequest request) {
        return teilnahmeService.create(request);
    }

    /** UC-016: Teilnahme bestätigen/anpassen (PARTEI nur die eigene, ORGANISATOR alle). */
    @PutMapping("/{id}")
    public TeilnahmeResponse update(@PathVariable Long id, @Valid @RequestBody TeilnahmeUpdateRequest request) {
        return teilnahmeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        teilnahmeService.delete(id);
    }
}
