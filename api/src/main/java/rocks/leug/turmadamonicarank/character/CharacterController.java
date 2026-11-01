package rocks.leug.turmadamonicarank.character;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.common.ApiFieldError;
import rocks.leug.turmadamonicarank.common.PageResponse;

@RestController
@RequestMapping("/api/v1/characters")
public class CharacterController {

    private final CharacterDirectory directory;

    public CharacterController(CharacterDirectory directory) {
        this.directory = directory;
    }

    @GetMapping
    public ResponseEntity<PageResponse<CharacterSummary>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        validatePage(limit, offset);
        PageResponse<CharacterSummary> page = directory.search(q, limit, offset);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                .body(page);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CharacterDetail> detail(@PathVariable String slug) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(directory.detail(slug));
    }

    private static void validatePage(int limit, int offset) {
        if (limit < 1 || limit > 100) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "The page size is invalid.",
                    List.of(new ApiFieldError(
                            "limit",
                            ApiErrorCode.VALIDATION_FAILED.name(),
                            "must be between 1 and 100")));
        }
        if (offset < 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "The page offset is invalid.",
                    List.of(new ApiFieldError(
                            "offset",
                            ApiErrorCode.VALIDATION_FAILED.name(),
                            "must be zero or greater")));
        }
    }
}
