package rocks.leug.turmadamonicarank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String from) {

    public MailProperties {
        Objects.requireNonNull(from, "app.mail.from is required");
    }
}
