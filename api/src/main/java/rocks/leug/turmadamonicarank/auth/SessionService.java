package rocks.leug.turmadamonicarank.auth;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.mail.AuthMailer;
import rocks.leug.turmadamonicarank.user.User;
import rocks.leug.turmadamonicarank.user.UserRepository;
import rocks.leug.turmadamonicarank.user.UserSelfResponse;
import rocks.leug.turmadamonicarank.user.UserStatus;

@Service
public class SessionService {

    public record SessionResult(TokenResponse token, String refreshToken) {
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final String dummyPasswordHash;

    public SessionService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public SessionResult login(String rawEmail, String password) {
        String email = EmailNormalizer.normalize(rawEmail);
        User user = userRepository.findByEmail(email).orElse(null);
        String hash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(password, hash);

        if (user == null || !passwordMatches) {
            throw new ApiException(
                    ApiErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "This account is disabled.");
        }
        return startSession(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public SessionResult refresh(String rawRefreshToken) {
        RefreshTokenService.IssuedRefreshToken rotated =
                refreshTokenService.rotate(rawRefreshToken);
        return startSession(rotated.user(), rotated.rawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    @Transactional(readOnly = true)
    public UserSelfResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.AUTH_REQUIRED, "Authentication is required."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "This account is disabled.");
        }
        return UserSelfResponse.from(user);
    }

    private SessionResult startSession(User user) {
        return startSession(user, refreshTokenService.issue(user).rawToken());
    }

    private SessionResult startSession(User user, String refreshToken) {
        JwtService.IssuedAccessToken accessToken = jwtService.issue(user);
        TokenResponse token = TokenResponse.bearer(
                accessToken.value(),
                accessToken.expiresIn(),
                UserSelfResponse.from(user));
        return new SessionResult(token, refreshToken);
    }
}
