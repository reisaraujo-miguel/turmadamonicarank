package rocks.leug.turmadamonicarank.auth;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailRequest(@NotBlank String token) {
}
