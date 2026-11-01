package rocks.leug.turmadamonicarank.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ProblemDetailsAdvice {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

    private final ProblemResponseWriter writer;

    public ProblemDetailsAdvice(ProblemResponseWriter writer) {
        this.writer = writer;
    }

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
        ProblemDetail problem = writer.problem(request, exception.code(), exception.getMessage());
        if (!exception.fieldErrors().isEmpty()) {
            problem.setProperty("errors", exception.fieldErrors());
        }
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBodyValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiFieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        ProblemDetail problem = writer.problem(
                request, ApiErrorCode.VALIDATION_FAILED, "One or more fields are invalid.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        List<ApiFieldError> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiFieldError(
                                result.getMethodParameter().getParameterName(),
                                ApiErrorCode.VALIDATION_FAILED.name(),
                                error.getDefaultMessage())))
                .toList();
        ProblemDetail problem = writer.problem(
                request, ApiErrorCode.VALIDATION_FAILED, "One or more fields are invalid.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<ApiFieldError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiFieldError(
                        violation.getPropertyPath().toString(),
                        ApiErrorCode.VALIDATION_FAILED.name(),
                        violation.getMessage()))
                .toList();
        ProblemDetail problem = writer.problem(
                request, ApiErrorCode.VALIDATION_FAILED, "One or more fields are invalid.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return writer.problem(
                request,
                org.springframework.http.HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_FAILED,
                "The request body is missing or malformed.");
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    ProblemDetail handleBadRequest(Exception exception, HttpServletRequest request) {
        return writer.problem(
                request,
                org.springframework.http.HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_FAILED,
                "A request parameter is invalid.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
        return writer.problem(request, ApiErrorCode.NOT_FOUND, "The requested resource was not found.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return writer.problem(
                request, ApiErrorCode.METHOD_NOT_ALLOWED, "The HTTP method is not supported here.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API error on {}", request.getRequestURI(), exception);
        return writer.problem(
                request, ApiErrorCode.INTERNAL_ERROR, "An unexpected error occurred.");
    }

    private ApiFieldError toFieldError(FieldError fieldError) {
        return new ApiFieldError(
                fieldError.getField(),
                ApiErrorCode.VALIDATION_FAILED.name(),
                fieldError.getDefaultMessage());
    }
}
