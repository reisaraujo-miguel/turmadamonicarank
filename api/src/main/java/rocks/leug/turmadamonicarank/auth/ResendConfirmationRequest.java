package rocks.leug.turmadamonicarank.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendConfirmationRequest(
        @NotBlank @Email @Size(max = 254) String email) {
}
