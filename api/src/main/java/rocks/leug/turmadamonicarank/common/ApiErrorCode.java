package rocks.leug.turmadamonicarank.common;

import java.util.Locale;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed"),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    AUTH_EMAIL_NOT_CONFIRMED(HttpStatus.FORBIDDEN, "Email not confirmed"),
    AUTH_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account disabled"),
    AUTH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Invalid token"),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "Invalid session"),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    AUTH_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email already registered"),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "Character not found"),
    BALLOT_INVALID_SIZE(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid ballot size"),
    BALLOT_DUPLICATE_CHARACTER(HttpStatus.UNPROCESSABLE_ENTITY, "Duplicate character"),
    BALLOT_CHARACTER_UNKNOWN(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown character"),
    BALLOT_CHARACTER_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Inactive character"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");

    private final HttpStatus status;
    private final String title;

    ApiErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String slug() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
