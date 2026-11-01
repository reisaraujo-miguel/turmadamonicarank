package rocks.leug.turmadamonicarank.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.user.UserSelfResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final SessionService sessionService;
    private final PasswordResetService passwordResetService;
    private final RefreshCookieFactory cookieFactory;

    public AuthController(
            RegistrationService registrationService,
            SessionService sessionService,
            PasswordResetService passwordResetService,
            RefreshCookieFactory cookieFactory) {
        this.registrationService = registrationService;
        this.sessionService = sessionService;
        this.passwordResetService = passwordResetService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return new RegisterResponse(UserSelfResponse.from(
                registrationService.register(
                        request.email(), request.password(), request.displayName())));
    }

    @PostMapping("/confirm-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        registrationService.confirm(request.token());
    }

    @PostMapping("/resend-confirmation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendConfirmation(@Valid @RequestBody ResendConfirmationRequest request) {
        registrationService.resend(request.email());
    }

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        SessionService.SessionResult session =
                sessionService.login(request.email(), request.password());
        cookieFactory.addTo(response, session.refreshToken());
        return session.token();
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false)
                    String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(
                    ApiErrorCode.AUTH_REFRESH_INVALID, "The session is invalid or expired.");
        }
        try {
            SessionService.SessionResult session = sessionService.refresh(refreshToken);
            cookieFactory.addTo(response, session.refreshToken());
            return session.token();
        } catch (ApiException exception) {
            cookieFactory.clear(response);
            throw exception;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false)
                    String refreshToken,
            HttpServletResponse response) {
        sessionService.logout(refreshToken);
        cookieFactory.clear(response);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.request(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.password());
    }

    @GetMapping("/me")
    public UserSelfResponse me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ApiException(ApiErrorCode.AUTH_REQUIRED, "Authentication is required.");
        }
        try {
            return sessionService.me(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ApiErrorCode.AUTH_REQUIRED, "Authentication is required.");
        }
    }
}
