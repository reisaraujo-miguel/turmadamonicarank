package rocks.leug.turmadamonicarank.auth;

import rocks.leug.turmadamonicarank.user.UserSelfResponse;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSelfResponse user) {

    public static TokenResponse bearer(String accessToken, long expiresIn, UserSelfResponse user) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, user);
    }
}
