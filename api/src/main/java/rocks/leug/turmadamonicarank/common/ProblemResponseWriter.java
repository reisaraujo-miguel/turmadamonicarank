package rocks.leug.turmadamonicarank.common;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ApiErrorCode code,
            String detail)
            throws IOException {
        ProblemDetail problem = problem(request, code, detail);
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    public ProblemDetail problem(HttpServletRequest request, ApiErrorCode code, String detail) {
        return problem(request, code.status(), code, detail);
    }

    public ProblemDetail problem(
            HttpServletRequest request,
            org.springframework.http.HttpStatus status,
            ApiErrorCode code,
            String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(code.title());
        problem.setType(URI.create("https://turmadamonicarank.example/errors/" + code.slug()));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        return problem;
    }
}
