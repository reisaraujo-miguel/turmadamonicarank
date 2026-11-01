package rocks.leug.turmadamonicarank.user;

import java.time.Instant;
import java.util.UUID;

public record UserSelfResponse(
        UUID id,
        String email,
        String displayName,
        boolean emailConfirmed,
        Instant createdAt) {

    public static UserSelfResponse from(User user) {
        return new UserSelfResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEmailConfirmed(),
                user.getCreatedAt());
    }
}
