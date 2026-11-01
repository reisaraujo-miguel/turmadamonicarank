package rocks.leug.turmadamonicarank.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.config.AuthProperties;
import rocks.leug.turmadamonicarank.user.User;

@Service
public class AuthTokenService {

    private final AuthTokenRepository repository;
    private final SecureTokens secureTokens;
    private final AuthProperties properties;
    private final Clock clock;

    public AuthTokenService(
            AuthTokenRepository repository,
            SecureTokens secureTokens,
            AuthProperties properties,
            Clock clock) {
        this.repository = repository;
        this.secureTokens = secureTokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public String issue(User user, AuthTokenPurpose purpose) {
        Instant now = clock.instant();
        List<AuthToken> outstanding = repository.findOutstanding(user, purpose);
        outstanding.forEach(token -> token.consume(now));

        String rawToken = secureTokens.generate();
        AuthToken token = new AuthToken(
                user,
                purpose,
                secureTokens.sha256(rawToken),
                now.plus(ttl(purpose)),
                now);
        repository.save(token);
        return rawToken;
    }

    @Transactional
    public AuthToken consume(String rawToken, AuthTokenPurpose purpose) {
        Instant now = clock.instant();
        AuthToken token = repository.findByTokenHash(secureTokens.sha256(rawToken))
                .filter(candidate -> candidate.getPurpose() == purpose)
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(AuthTokenService::invalidToken);
        token.consume(now);
        return token;
    }

    private Duration ttl(AuthTokenPurpose purpose) {
        return switch (purpose) {
            case EMAIL_CONFIRMATION -> properties.confirmationTtl();
            case PASSWORD_RESET -> properties.resetTtl();
        };
    }

    private static ApiException invalidToken() {
        return new ApiException(
                ApiErrorCode.AUTH_TOKEN_INVALID,
                "The token is invalid, expired or already used.");
    }
}
