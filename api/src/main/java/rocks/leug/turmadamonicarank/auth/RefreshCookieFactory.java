package rocks.leug.turmadamonicarank.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

import rocks.leug.turmadamonicarank.config.AuthProperties;

@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "tmr_refresh";
    public static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthProperties properties;

    public RefreshCookieFactory(AuthProperties properties) {
        this.properties = properties;
    }

    public void addTo(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(properties.refreshTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
