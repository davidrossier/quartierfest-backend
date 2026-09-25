package ch.quartierfest.backend.auth;

// UC-014: Benutzer anmelden (AUTH-002)

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record LoginRequest(@NotBlank String email, @NotBlank String passwort) {
    }

    public record LoginResponse(String token) {
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        // SEC-002: Client-IP für die Drosselung; in prod hinter Proxy via server.forward-headers-strategy=native
        return new LoginResponse(authService.login(request.email(), request.passwort(), http.getRemoteAddr()));
    }
}
