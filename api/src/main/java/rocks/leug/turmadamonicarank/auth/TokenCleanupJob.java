package rocks.leug.turmadamonicarank.auth;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final AuthTokenRepository authTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public TokenCleanupJob(
            AuthTokenRepository authTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            Clock clock) {
        this.authTokenRepository = authTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        var threshold = clock.instant().minus(Duration.ofDays(30));
        int confirmationTokens = authTokenRepository.deleteExpiredBefore(threshold);
        int refreshTokens = refreshTokenRepository.deleteExpiredBefore(threshold);
        if (confirmationTokens + refreshTokens > 0) {
            log.info(
                    "Deleted {} expired auth tokens and {} expired refresh tokens",
                    confirmationTokens,
                    refreshTokens);
        }
    }
}
