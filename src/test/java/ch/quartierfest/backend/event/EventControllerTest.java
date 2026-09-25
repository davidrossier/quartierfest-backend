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
import static org.mockito.ArgumentMatchers.eq;
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

    private EventResponse buildEvent(String alternativerStandort) {
        return new EventResponse(1L, LocalDate.of(2025, 7, 5), LocalTime.of(15, 0), "Buchlenwiese",
                alternativerStandort, null, null);
    }

    private EventRequest buildRequest(String alternativerStandort) {
        return new EventRequest(LocalDate.of(2025, 7, 5), LocalTime.of(15, 0), "Buchlenwiese",
                alternativerStandort, null, null);
    }

    @Test
    @DisplayName("UC-003: GET /api/events gibt alle Events zurück")
    void getAll_returnsList() throws Exception {
        when(eventService.findAll()).thenReturn(List.of(buildEvent(null)));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].standort").value("Buchlenwiese"));
    }

    @Test
    @DisplayName("UC-003: POST /api/events legt einen Event an und gibt ihn zurück")
    void create_returnsSavedEvent() throws Exception {
        when(eventService.create(any(EventRequest.class))).thenReturn(buildEvent(null));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.standort").value("Buchlenwiese"));
    }

    @Test
    @DisplayName("UC-003: PUT /api/events/{id} aktualisiert einen Event")
    void update_returnsUpdatedEvent() throws Exception {
        when(eventService.update(eq(1L), any(EventRequest.class))).thenReturn(buildEvent("Turnhalle"));

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Turnhalle"))))
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
