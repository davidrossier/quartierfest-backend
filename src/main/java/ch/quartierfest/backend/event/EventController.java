package ch.quartierfest.backend.event;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public List<Event> findAll() {
        return eventService.findAll();
    }

    @PostMapping
    public Event create(@RequestBody Event event) {
        return eventService.save(event);
    }

    @PutMapping("/{id}")
    public Event update(@PathVariable Long id, @RequestBody Event event) {
        event.setId(id);
        return eventService.save(event);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }
}
