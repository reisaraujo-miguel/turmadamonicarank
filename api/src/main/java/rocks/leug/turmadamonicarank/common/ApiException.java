package rocks.leug.turmadamonicarank.common;

import java.util.List;

public class ApiException extends RuntimeException {

    private final ApiErrorCode code;
    private final List<ApiFieldError> fieldErrors;

    public ApiException(ApiErrorCode code, String detail) {
        this(code, detail, List.of());
    }

    public ApiException(ApiErrorCode code, String detail, List<ApiFieldError> fieldErrors) {
        super(detail);
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public ApiErrorCode code() {
        return code;
    }

    public List<ApiFieldError> fieldErrors() {
        return fieldErrors;
    }
}
