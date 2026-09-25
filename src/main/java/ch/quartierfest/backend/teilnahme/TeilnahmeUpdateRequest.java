package ch.quartierfest.backend.teilnahme;

// UC-016: Whitelist-DTO für PUT /api/teilnahmen/{id} — bewusst kein Entity-Binding,
// damit die einladung-Verknüpfung über diesen Endpunkt nie veränderbar ist.

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record TeilnahmeUpdateRequest(
        @Nullable Integer anzahlPersonenEffektiv,
        @Nullable Boolean hilftAufstellen,
        @Nullable Boolean hilftAufraumen,
        @Valid @Nullable List<TeilnahmeBuffetBeitrag> buffetBeitraege) {
}
