package rocks.leug.turmadamonicarank.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;

@Component
public class CurrentUser {

    public UUID require(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ApiException(ApiErrorCode.AUTH_REQUIRED, "Authentication is required.");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ApiErrorCode.AUTH_REQUIRED, "Authentication is required.");
        }
    }
}
