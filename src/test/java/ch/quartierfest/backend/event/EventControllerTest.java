package ch.quartierfest.backend.event;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Unit tests for UC-003 – Event anlegen. */
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    private Event buildEvent() {
        Event e = new Event();
        e.setId(1L);
        e.setDatum(LocalDate.of(2025, 7, 5));
        e.setStartzeit(LocalTime.of(15, 0));
        e.setStandort("Buchlenwiese");
        return e;
    }

    @Test
    @DisplayName("UC-003: GET /api/events gibt alle Events zurück")
    void getAll_returnsList() throws Exception {
        when(eventService.findAll()).thenReturn(List.of(buildEvent()));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].standort").value("Buchlenwiese"));
    }

    @Test
    @DisplayName("UC-003: POST /api/events legt einen Event an und gibt ihn zurück")
    void create_returnsSavedEvent() throws Exception {
        Event e = buildEvent();
        when(eventService.save(any(Event.class))).thenReturn(e);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(e)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.standort").value("Buchlenwiese"));
    }

    @Test
    @DisplayName("UC-003: PUT /api/events/{id} aktualisiert einen Event")
    void update_returnsUpdatedEvent() throws Exception {
        Event e = buildEvent();
        e.setAlternativerStandort("Turnhalle");
        when(eventService.save(any(Event.class))).thenReturn(e);

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(e)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alternativerStandort").value("Turnhalle"));
    }

    @Test
    @DisplayName("UC-003: DELETE /api/events/{id} löscht einen Event")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isOk());
    }
}
