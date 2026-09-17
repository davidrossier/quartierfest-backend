package ch.quartierfest.backend;

/**
 * Traceability:
 *   UC: übergreifend (API-001 – expliziter API-Contract)
 *   TCs: TC-046
 *   Last traced: 2026-09-17
 */

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract-Abgleich (TC-046, API-001 Stufe 1): die von springdoc erzeugte OpenAPI-Spec
 * ({@code GET /v3/api-docs}) muss byte-identisch mit der eingecheckten {@code specs/openapi.json}
 * sein. Ändert sich der Contract (Entity, Controller, DTO), wird die Datei mit
 * <pre>OPENAPI_UPDATE=true ./mvnw verify -Dit.test=OpenApiContractIT</pre>
 * neu geschrieben und im selben PR committet. Das Frontend generiert daraus seine Typen
 * (openapi-typescript) und prüft in seiner CI gegen diese Datei auf Drift.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("dev")
class OpenApiContractIT {

    static final Path SPEC = Path.of("specs", "openapi.json");
    static final String UPDATE_ENV = "OPENAPI_UPDATE";

    @LocalServerPort private int port;
    @Autowired private ObjectMapper objectMapper;   // Jackson 3.x (tools.jackson)

    @Test
    @DisplayName("TC-046 – /v3/api-docs entspricht der eingecheckten specs/openapi.json")
    void tc046_openApiSpecEntsprichtEingechecktemContract() throws IOException {
        String live = new RestTemplate().getForObject("http://localhost:" + port + "/v3/api-docs", String.class);
        String normalisiert = normalisieren(live);

        if ("true".equalsIgnoreCase(System.getenv(UPDATE_ENV))) {
            Files.createDirectories(SPEC.getParent());
            Files.writeString(SPEC, normalisiert, StandardCharsets.UTF_8);
            return;
        }

        assertThat(SPEC)
            .withFailMessage("specs/openapi.json fehlt – mit %s=true ./mvnw verify -Dit.test=OpenApiContractIT erzeugen", UPDATE_ENV)
            .exists();
        String eingecheckt = Files.readString(SPEC, StandardCharsets.UTF_8);
        assertThat(normalisiert)
            .withFailMessage(() -> "API-Contract weicht von specs/openapi.json ab. Datei mit "
                + UPDATE_ENV + "=true ./mvnw verify -Dit.test=OpenApiContractIT neu erzeugen und mitcommitten.\n"
                + "Erwartet (eingecheckt):\n" + eingecheckt + "\nTatsächlich (live):\n" + normalisiert)
            .isEqualTo(eingecheckt);
    }

    /** Pretty-Print mit sortierten Keys und abschliessendem Zeilenumbruch → stabiles git diff. */
    private String normalisieren(String json) {
        JsonNode tree = objectMapper.readTree(json);
        ObjectMapper stabil = objectMapper.rebuild()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();
        // JsonNode-Serialisierung ignoriert ORDER_MAP_ENTRIES_BY_KEYS → über Map gehen
        Object asMap = stabil.convertValue(tree, java.util.Map.class);
        return stabil.writeValueAsString(asMap) + "\n";
    }
}
