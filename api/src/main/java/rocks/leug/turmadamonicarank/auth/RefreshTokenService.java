package rocks.leug.turmadamonicarank.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.config.AuthProperties;
import rocks.leug.turmadamonicarank.user.User;

@Service
public class RefreshTokenService {

    public record IssuedRefreshToken(String rawToken, User user) {
    }

    private final RefreshTokenRepository repository;
    private final SecureTokens secureTokens;
    private final AuthProperties properties;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            SecureTokens secureTokens,
            AuthProperties properties,
            Clock clock) {
        this.repository = repository;
        this.secureTokens = secureTokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        Instant now = clock.instant();
        String rawToken = secureTokens.generate();
        RefreshToken token = new RefreshToken(
                user,
                UUID.randomUUID(),
                secureTokens.sha256(rawToken),
                now.plus(properties.refreshTtl()),
                now);
        repository.save(token);
        return new IssuedRefreshToken(rawToken, user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public IssuedRefreshToken rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = repository.findByTokenHash(secureTokens.sha256(rawToken))
                .orElseThrow(RefreshTokenService::invalidSession);

        if (current.getRevokedAt() != null) {
            repository.revokeFamily(current.getFamilyId(), now);
            throw invalidSession();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw invalidSession();
        }

        String nextRawToken = secureTokens.generate();
        RefreshToken next = new RefreshToken(
                current.getUser(),
                current.getFamilyId(),
                secureTokens.sha256(nextRawToken),
                now.plus(properties.refreshTtl()),
                now);
        repository.save(next);
        current.revoke(now, next.getId());
        return new IssuedRefreshToken(nextRawToken, current.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(secureTokens.sha256(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    @Transactional
    public void revokeAllFor(User user) {
        repository.revokeAllForUser(user.getId(), clock.instant());
    }

    private static ApiException invalidSession() {
        return new ApiException(
                ApiErrorCode.AUTH_REFRESH_INVALID,
                "The session is invalid or expired.");
    }
}
