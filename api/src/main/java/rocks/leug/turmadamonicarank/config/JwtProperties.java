package rocks.leug.turmadamonicarank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret, String issuer, String audience, Duration accessTtl) {

    public JwtProperties {
        Objects.requireNonNull(issuer, "app.security.jwt.issuer is required");
        Objects.requireNonNull(audience, "app.security.jwt.audience is required");
        Objects.requireNonNull(accessTtl, "app.security.jwt.access-ttl is required");
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "app.security.jwt.secret must be at least 32 bytes; set JWT_SECRET");
        }
    }
}
