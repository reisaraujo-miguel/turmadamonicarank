package rocks.leug.turmadamonicarank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.web")
public record WebProperties(String baseUrl) {

    public WebProperties {
        Objects.requireNonNull(baseUrl, "app.web.base-url is required");
        validate(baseUrl);
    }

    private static void validate(String baseUrl) {
        URI uri = URI.create(baseUrl);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("app.web.base-url must be an absolute http(s) URL");
        }
    }

    public URI baseUri() {
        URI uri = URI.create(baseUrl);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("app.web.base-url must be an absolute http(s) URL");
        }
        return uri;
    }

    public String linkBase() {
        return baseUrl.replaceAll("/+$", "");
    }
}
