package ch.quartierfest.backend.mahnung;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/mahnungen", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MahnungController {

    private final MahnungService mahnungService;

    @GetMapping
    public List<MahnungResponse> findAll() {
        return mahnungService.findAll();
    }

    @PostMapping
    public MahnungResponse create(@Valid @RequestBody MahnungRequest request) {
        return mahnungService.create(request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mahnungService.delete(id);
    }
}
