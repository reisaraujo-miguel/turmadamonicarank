package rocks.leug.turmadamonicarank.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ProductionConfigGuard {

    private static final String DEV_DEFAULT_SECRET =
            "local-development-only-secret-change-me-32b";

    public ProductionConfigGuard(Environment environment, JwtProperties jwtProperties) {
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        if (production && DEV_DEFAULT_SECRET.equals(jwtProperties.secret())) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to a production value before starting with the prod profile");
        }
    }
}
