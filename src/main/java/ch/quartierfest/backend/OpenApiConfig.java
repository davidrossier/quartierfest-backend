package ch.quartierfest.backend;

// API-001 (Stufe 1): OpenAPI-Metadaten und Bearer-Schema für die Swagger-UI.
// Der Ist-Contract selbst wird von springdoc aus den Controllern/Entities abgeleitet
// und in specs/openapi.json versioniert (Abgleich: OpenApiContractIT, TC-046).

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI quartierfestOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Quartierfest API")
                .description("REST-API des Quartierfest-Backends. Authentifizierung via POST /api/auth/login (HS256-JWT, AUTH-002).")
                .version("0.0.1"))
            .components(new Components().addSecuritySchemes(BEARER_AUTH,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
