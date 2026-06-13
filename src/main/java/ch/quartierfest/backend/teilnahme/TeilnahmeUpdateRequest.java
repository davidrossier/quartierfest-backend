package ch.quartierfest.backend.teilnahme;

// UC-016: Whitelist-DTO für PUT /api/teilnahmen/{id} — bewusst kein Entity-Binding,
// damit die einladung-Verknüpfung über diesen Endpunkt nie veränderbar ist.

import java.util.List;

public record TeilnahmeUpdateRequest(
        Integer anzahlPersonenEffektiv,
        Boolean hilftAufstellen,
        Boolean hilftAufraumen,
        List<TeilnahmeBuffetBeitrag> buffetBeitraege) {
}
