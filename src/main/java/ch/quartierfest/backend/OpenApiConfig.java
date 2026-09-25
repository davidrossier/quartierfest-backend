package ch.quartierfest.backend;

// API-001: OpenAPI-Metadaten, Bearer-Schema für die Swagger-UI und Contract-Präzisierungen.
// Der Contract selbst wird von springdoc aus Controllern und Request-/Response-Records abgeleitet
// und in specs/openapi.json versioniert (Abgleich: OpenApiContractIT, TC-046).

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";
    private static final String API_ERROR = "ApiError";

    static {
        // API-001 Stufe 2: Enums als eigene Schemas statt inline je Property → ein gemeinsamer Typ im Frontend
        ModelResolver.enumsAsRef = true;
    }

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

    /**
     * API-001 Stufe 2 (E7): In Response-Records ({@code *Response}, {@code *Kurz}) sind alle Felder Pflicht,
     * ausser sie sind mit {@link Nullable} markiert. Nullbare Felder lässt Jackson weg
     * ({@code spring.jackson.default-property-inclusion=non_null}), sie sind im Contract also optional.
     * springdoc selbst kennt nur Bean-Validation — ohne diesen Customizer wäre jedes Response-Feld optional.
     */
    @Bean
    public OpenApiCustomizer responsePflichtfelder() {
        Map<String, Class<?>> records = responseRecords();
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas == null) {
                return;
            }
            schemas.forEach((name, schema) -> {
                Class<?> typ = records.get(name);
                if (typ == null || schema.getProperties() == null) {
                    return;
                }
                List<String> pflicht = Arrays.stream(typ.getRecordComponents())
                    .filter(komponente -> !istNullable(komponente))
                    .map(RecordComponent::getName)
                    .filter(schema.getProperties()::containsKey)
                    .sorted()
                    .toList();
                schema.setRequired(pflicht.isEmpty() ? null : pflicht);
            });
        };
    }

    /**
     * API-001 Stufe 2: Fehler-Schema {status, message} (ERROR-001) in die Spec aufnehmen und allen
     * Operationen als 4XX-Antwort anhängen. Welche Codes konkret auftreten, steht im Testdesign.
     */
    @Bean
    public OpenApiCustomizer fehlerSchema() {
        return openApi -> {
            ModelConverters.getInstance().read(GlobalExceptionHandler.ApiError.class)
                .forEach(openApi.getComponents()::addSchemas);
            openApi.getComponents().getSchemas().get(API_ERROR).type("object").required(List.of("message", "status"));
            ApiResponse fehler = new ApiResponse()
                .description("Fehler im einheitlichen Format (ERROR-001)")
                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + API_ERROR))));
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pfad -> pfad.readOperations()
                    .forEach(operation -> operation.getResponses().addApiResponse("4XX", fehler)));
            }
        };
    }

    private static boolean istNullable(RecordComponent komponente) {
        return komponente.isAnnotationPresent(Nullable.class)
            || komponente.getAnnotatedType().isAnnotationPresent(Nullable.class);
    }

    private static Map<String, Class<?>> responseRecords() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((reader, factory) -> {
            String name = reader.getClassMetadata().getClassName();
            return "java.lang.Record".equals(reader.getClassMetadata().getSuperClassName())
                && (name.endsWith("Response") || name.endsWith("Kurz"));
        });
        Map<String, Class<?>> records = new HashMap<>();
        scanner.findCandidateComponents(OpenApiConfig.class.getPackageName()).forEach(bean -> {
            Class<?> typ = ClassUtils.resolveClassName(bean.getBeanClassName(), OpenApiConfig.class.getClassLoader());
            records.put(typ.getSimpleName(), typ);
        });
        return records;
    }
}
