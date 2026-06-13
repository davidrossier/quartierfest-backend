package ch.quartierfest.backend.teilnahme;

// UC-016: Ownership-Prüfung für @PreAuthorize — ORGANISATOR darf alles,
// PARTEI nur die Teilnahme der eigenen Partei (JWT sub → Benutzer → Partei).

import ch.quartierfest.backend.benutzer.Benutzer;
import ch.quartierfest.backend.benutzer.BenutzerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("teilnahmeZugriff")
@RequiredArgsConstructor
public class TeilnahmeZugriff {

    private final TeilnahmeRepository teilnahmeRepository;
    private final BenutzerRepository benutzerRepository;

    public boolean darfBearbeiten(Long teilnahmeId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        boolean istOrganisator = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ORGANISATOR".equals(a.getAuthority()));
        if (istOrganisator) {
            return true;
        }
        Long benutzerId = parseSub(authentication.getName());
        if (benutzerId == null) {
            return false;
        }
        return benutzerRepository.findById(benutzerId)
                .map(Benutzer::getPartei)
                .flatMap(partei -> teilnahmeRepository.findById(teilnahmeId)
                        .map(t -> t.getEinladung().getPartei().getId().equals(partei.getId())))
                .orElse(false);
    }

    private Long parseSub(String sub) {
        try {
            return Long.valueOf(sub);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
