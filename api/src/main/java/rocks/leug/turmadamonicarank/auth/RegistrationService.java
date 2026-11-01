package rocks.leug.turmadamonicarank.auth;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.mail.AuthMailer;
import rocks.leug.turmadamonicarank.user.User;
import rocks.leug.turmadamonicarank.user.UserRepository;
import rocks.leug.turmadamonicarank.user.UserStatus;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final AuthMailer authMailer;
    private final Clock clock;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            AuthMailer authMailer,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.authMailer = authMailer;
        this.clock = clock;
    }

    @Transactional
    public User register(String rawEmail, String password, String rawDisplayName) {
        String email = EmailNormalizer.normalize(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw emailAlreadyRegistered();
        }

        Instant now = clock.instant();
        User user = new User(
                email,
                passwordEncoder.encode(password),
                normalizeDisplayName(rawDisplayName),
                now);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyRegistered();
        }

        try {
            String token = authTokenService.issue(user, AuthTokenPurpose.EMAIL_CONFIRMATION);
            authMailer.sendConfirmation(user, token);
        } catch (MailException exception) {
            log.warn("Could not send a confirmation email", exception);
        }
        return user;
    }

    @Transactional
    public void confirm(String rawToken) {
        AuthToken token = authTokenService.consume(rawToken, AuthTokenPurpose.EMAIL_CONFIRMATION);
        User user = token.getUser();
        if (!user.isEmailConfirmed()) {
            user.confirmEmail(clock.instant());
        }
    }

    @Transactional
    public void resend(String rawEmail) {
        String email = EmailNormalizer.normalize(rawEmail);
        userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> !user.isEmailConfirmed())
                .ifPresent(user -> {
                    try {
                        String token = authTokenService.issue(
                                user, AuthTokenPurpose.EMAIL_CONFIRMATION);
                        authMailer.sendConfirmation(user, token);
                    } catch (MailException exception) {
                        log.warn("Could not resend a confirmation email", exception);
                    }
                });
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        return displayName.trim();
    }

    private static ApiException emailAlreadyRegistered() {
        return new ApiException(
                ApiErrorCode.AUTH_EMAIL_ALREADY_REGISTERED,
                "This email is already registered.");
    }
}
