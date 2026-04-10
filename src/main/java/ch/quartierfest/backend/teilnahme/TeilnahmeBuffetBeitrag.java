package ch.quartierfest.backend.teilnahme;

import ch.quartierfest.backend.einladung.Einladung.BuffetBeitrag;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeilnahmeBuffetBeitrag {

    @Enumerated(EnumType.STRING)
    private BuffetBeitrag art;

    private String beschreibung;
}
