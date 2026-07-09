package ch.quartierfest.backend;

// AUTH-001/AUTH-002: Absicherung der API. Eigenbau-Login (UC-014): das Backend
// stellt JWTs selbst aus (HS256, symmetrischer Schlüssel) und validiert sie als
// OAuth2 Resource Server. Rollen-Claim "rolle" → ROLE_ORGANISATOR / ROLE_PARTEI.

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    /**
     * Autorisierungsmatrix — Default (fail-closed, SEC-001): Login offen,
     * Benutzerverwaltung nur ORGANISATOR, PARTEI ausschliesslich auf den eigenen
     * Teilnahme-Endpunkten (Ownership zusätzlich via Methoden-Security), alles
     * übrige nur ORGANISATOR. Gilt für prod, security-test und jeden Start ohne
     * explizites dev-Profil.
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/teilnahmen/meine").hasAnyRole("ORGANISATOR", "PARTEI")
                .requestMatchers(HttpMethod.PUT, "/api/teilnahmen/*").hasAnyRole("ORGANISATOR", "PARTEI")
                .requestMatchers("/api/**").hasRole("ORGANISATOR")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Nur bei explizitem dev-Profil (SEC-001): alles erlaubt, aber Bearer-Tokens
     * werden trotzdem verarbeitet — damit funktionieren GET /api/teilnahmen/meine
     * und die Ownership-Prüfung (@PreAuthorize) auch lokal und in den dev-ITs.
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain openFilterChain(HttpSecurity http,
                                               JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        log.warn("Offene Security-Chain aktiv (Profil 'dev'): alle Endpunkte permitAll() — nur für lokale Entwicklung!");
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Mappt den Claim "rolle" auf ROLE_* — der Spring-Default liest nur "scope". */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rolle = jwt.getClaimAsString("rolle");
            return rolle == null ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rolle));
        });
        return converter;
    }

    private SecretKey secretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
