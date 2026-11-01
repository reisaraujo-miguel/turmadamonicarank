package rocks.leug.turmadamonicarank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.security.auth")
public record AuthProperties(
        Duration refreshTtl,
        Duration confirmationTtl,
        Duration resetTtl,
        boolean refreshCookieSecure) {

    public AuthProperties {
        Objects.requireNonNull(refreshTtl, "app.security.auth.refresh-ttl is required");
        Objects.requireNonNull(confirmationTtl, "app.security.auth.confirmation-ttl is required");
        Objects.requireNonNull(resetTtl, "app.security.auth.reset-ttl is required");
    }
}
