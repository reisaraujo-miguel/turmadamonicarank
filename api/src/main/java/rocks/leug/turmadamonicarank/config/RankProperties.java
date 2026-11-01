package rocks.leug.turmadamonicarank.config;

import java.time.ZoneId;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rank")
public record RankProperties(ZoneId zone) {

    public RankProperties {
        Objects.requireNonNull(zone, "app.rank.zone is required");
    }
}
