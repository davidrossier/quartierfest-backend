package ch.quartierfest.backend;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * ERROR-001: Einheitliches Fehlerformat {status, message} für alle REST-Endpunkte.
 *
 * Erbt von {@link ResponseEntityExceptionHandler}, damit auch die von Spring MVC
 * vorbehandelten Exceptions (ResponseStatusException, ungültiges JSON → 400,
 * 405, 415, unbekannter Pfad → 404, ...) ihren Status behalten und nicht vom
 * Fallback-Handler zu 500ern werden — {@link #handleExceptionInternal} formt
 * sie alle in das einheitliche Format um.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public record ApiError(int status, String message) {}

    /** 404 — Entity zu einer ID nicht gefunden (z.B. Lazy-Referenz auf gelöschten Datensatz). */
    @ExceptionHandler({EntityNotFoundException.class, JpaObjectRetrievalFailureException.class})
    ResponseEntity<ApiError> handleEntityNotFound(Exception e) {
        return error(HttpStatus.NOT_FOUND, "Datensatz nicht gefunden.");
    }

    /**
     * 409 — DB-Constraint verletzt. Unique-Verletzung (DB-002: Duplikat-Einladung/-Teilnahme/-Abrechnung)
     * und FK-Verletzung (nicht-existente ID, Löschen eines referenzierten Datensatzes) bekommen
     * unterschiedliche, aber DB-detailfreie Meldungen.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException e) {
        if (istUniqueVerletzung(e)) {
            return error(HttpStatus.CONFLICT, "Datensatz existiert bereits.");
        }
        return error(HttpStatus.CONFLICT, "Referenzierter Datensatz existiert nicht oder wird noch verwendet.");
    }

    private static boolean istUniqueVerletzung(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve
                    && cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
                return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
    }

    /**
     * 403 aus {@code @PreAuthorize} weiterwerfen statt behandeln: unbehandelt erreicht die
     * Exception den ExceptionTranslationFilter von Spring Security, der 403/401 korrekt setzt.
     * Ohne diesen Handler würde der Exception-Fallback daraus einen 500er machen.
     */
    @ExceptionHandler(AccessDeniedException.class)
    void handleAccessDenied(AccessDeniedException e) throws AccessDeniedException {
        throw e;
    }

    /** 500 — unerwarteter Fehler: generische Meldung an den Client, Details nur ins Log. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception e) {
        logger.error("Unbehandelte Exception", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Serverfehler.");
    }

    /** 400 — Bean-Validation-Fehler mit Feldliste (alphabetisch, für stabile Assertions). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String felder = ex.getBindingResult().getFieldErrors().stream()
                .map(fehler -> fehler.getField() + ": " + fehler.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(new ApiError(status.value(), "Validierung fehlgeschlagen: " + felder),
                headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = null;
        if (body instanceof ProblemDetail problemDetail) {
            message = problemDetail.getDetail();
        } else if (ex instanceof ErrorResponse errorResponse) {
            message = errorResponse.getBody().getDetail();
        }
        if (message == null || message.isBlank()) {
            message = statusCode instanceof HttpStatus status ? status.getReasonPhrase() : ex.getMessage();
        }
        return new ResponseEntity<>(new ApiError(statusCode.value(), message), headers, statusCode);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message));
    }
}
