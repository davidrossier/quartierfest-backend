package ch.quartierfest.backend.person;

import org.jspecify.annotations.Nullable;

/** UC-001: Person in Antworten (API-001 Stufe 2). */
public record PersonResponse(
        Long id,
        String vorname,
        String name,
        @Nullable String telefonnummer,
        @Nullable String mobilenummer,
        @Nullable String email) {

    public static PersonResponse von(Person person) {
        return new PersonResponse(person.getId(), person.getVorname(), person.getName(),
                person.getTelefonnummer(), person.getMobilenummer(), person.getEmail());
    }
}
