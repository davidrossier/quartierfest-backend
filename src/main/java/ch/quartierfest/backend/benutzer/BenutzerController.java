package ch.quartierfest.backend.benutzer;

// UC-015: Benutzer verwalten (AUTH-002)

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/benutzer", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BenutzerController {

    private final BenutzerService benutzerService;

    public record PasswortReset(@NotBlank @Size(min = 10) String passwort) {
    }

    @GetMapping
    public List<BenutzerResponse> findAll() {
        return benutzerService.findAll();
    }

    @PostMapping
    public BenutzerResponse create(@Valid @RequestBody BenutzerRequest request) {
        return benutzerService.create(request);
    }

    @PutMapping("/{id}/passwort")
    public BenutzerResponse passwortSetzen(@PathVariable Long id, @Valid @RequestBody PasswortReset reset) {
        return benutzerService.passwortSetzen(id, reset.passwort());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        benutzerService.delete(id);
    }
}
