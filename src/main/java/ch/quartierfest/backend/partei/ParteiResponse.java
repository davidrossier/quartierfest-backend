package ch.quartierfest.backend.partei;

import ch.quartierfest.backend.person.PersonResponse;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** UC-002: Partei mit ihren Personen — nur in GET/POST/PUT /api/parteien (API-001 Stufe 2). */
public record ParteiResponse(
        Long id,
        String bezeichnung,
        String adresse,
        boolean twintAktiv,
        @Nullable String twintMobilenummer,
        List<PersonResponse> personen) {

    public static ParteiResponse von(Partei partei) {
        return new ParteiResponse(partei.getId(), partei.getBezeichnung(), partei.getAdresse(),
                partei.isTwintAktiv(), partei.getTwintMobilenummer(),
                partei.getPersonen().stream().map(PersonResponse::von).toList());
    }
}
