package rocks.leug.turmadamonicarank.security;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import rocks.leug.turmadamonicarank.auth.RefreshCookieFactory;
import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ProblemResponseWriter;
import rocks.leug.turmadamonicarank.config.WebProperties;

public class OriginCheckFilter extends OncePerRequestFilter {

    private final WebProperties webProperties;
    private final ProblemResponseWriter writer;

    public OriginCheckFilter(WebProperties webProperties, ProblemResponseWriter writer) {
        this.webProperties = webProperties;
        this.writer = writer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean cookieEndpoint = path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/logout");
        return !cookieEndpoint || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        if (hasRefreshCookie(request)) {
            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if (origin != null && !matchesBaseOrigin(origin)) {
                writer.write(
                        request,
                        response,
                        ApiErrorCode.FORBIDDEN,
                        "The request origin is not allowed.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (RefreshCookieFactory.COOKIE_NAME.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesBaseOrigin(String rawOrigin) {
        try {
            URI origin = URI.create(rawOrigin);
            URI base = webProperties.baseUri();
            return sameScheme(origin, base)
                    && sameHost(origin, base)
                    && effectivePort(origin) == effectivePort(base);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean sameScheme(URI left, URI right) {
        return left.getScheme() != null
                && left.getScheme().equalsIgnoreCase(right.getScheme());
    }

    private static boolean sameHost(URI left, URI right) {
        return left.getHost() != null && left.getHost().equalsIgnoreCase(right.getHost());
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
