package rocks.leug.turmadamonicarank.auth;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rocks.leug.turmadamonicarank.mail.AuthMailer;
import rocks.leug.turmadamonicarank.user.User;
import rocks.leug.turmadamonicarank.user.UserRepository;
import rocks.leug.turmadamonicarank.user.UserStatus;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthMailer authMailer;
    private final Clock clock;

    public PasswordResetService(
            UserRepository userRepository,
            AuthTokenService authTokenService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            AuthMailer authMailer,
            Clock clock) {
        this.userRepository = userRepository;
        this.authTokenService = authTokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.authMailer = authMailer;
        this.clock = clock;
    }

    @Transactional
    public void request(String rawEmail) {
        String email = EmailNormalizer.normalize(rawEmail);
        userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    try {
                        String token = authTokenService.issue(
                                user, AuthTokenPurpose.PASSWORD_RESET);
                        authMailer.sendPasswordReset(user, token);
                    } catch (MailException exception) {
                        log.warn("Could not send a password reset email", exception);
                    }
                });
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        AuthToken token = authTokenService.consume(rawToken, AuthTokenPurpose.PASSWORD_RESET);
        User user = token.getUser();
        user.changePassword(passwordEncoder.encode(newPassword), clock.instant());
        refreshTokenService.revokeAllFor(user);
    }
}
