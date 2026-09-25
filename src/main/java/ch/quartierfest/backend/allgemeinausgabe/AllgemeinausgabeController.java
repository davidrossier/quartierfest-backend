package ch.quartierfest.backend.allgemeinausgabe;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/allgemeinausgaben", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AllgemeinausgabeController {

    private final AllgemeinausgabeService allgemeinausgabeService;

    @GetMapping
    public List<AllgemeinausgabeResponse> findAll() {
        return allgemeinausgabeService.findAll();
    }

    @PostMapping
    public AllgemeinausgabeResponse create(@Valid @RequestBody AllgemeinausgabeRequest request) {
        return allgemeinausgabeService.create(request);
    }

    @PutMapping("/{id}")
    public AllgemeinausgabeResponse update(@PathVariable Long id, @Valid @RequestBody AllgemeinausgabeRequest request) {
        return allgemeinausgabeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        allgemeinausgabeService.delete(id);
    }
}
