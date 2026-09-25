package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.Einladung.BuffetBeitrag;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeilnahmeBuffetBeitrag {

    // API-001 Stufe 2: art ist fachlich immer gesetzt (Frontend-Präzisierung aus Stufe 1 ins Backend verlegt)
    @NotNull
    @Enumerated(EnumType.STRING)
    private BuffetBeitrag art;

    private @Nullable String beschreibung;
}
