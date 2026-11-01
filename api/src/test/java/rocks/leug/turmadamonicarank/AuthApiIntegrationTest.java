package rocks.leug.turmadamonicarank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

import tools.jackson.databind.JsonNode;

class AuthApiIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "copacabana-2026";
    private static final String NEW_PASSWORD = "nova-senha-forte";
    private static final Pattern CONFIRMATION_TOKEN =
            Pattern.compile("/confirmar-email\\?token=([A-Za-z0-9_-]+)");
    private static final Pattern RESET_TOKEN =
            Pattern.compile("/redefinir-senha\\?token=([A-Za-z0-9_-]+)");

    @Test
    void registerConfirmLoginMeAndRotateSession() throws Exception {
        String email = "ana@example.com";
        register(email, PASSWORD, "Ana");
        assertThat(mail.messages()).hasSize(1);
        confirm(confirmationToken());

        MvcResult login = login(email, PASSWORD);
        String accessToken = accessToken(login);
        assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Path=/api/v1/auth");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailConfirmed").value(true));

        Cookie firstCookie = refreshCookie(login);
        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        Cookie secondCookie = refreshCookie(refreshed);
        assertThat(secondCookie.getValue()).isNotEqualTo(firstCookie.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALID"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(secondCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refreshReuseRevokesTheFamilyInCommittedState() throws Exception {
        String email = "reuse-" + UUID.randomUUID() + "@example.com";
        register(email, PASSWORD, null);
        confirm(confirmationToken());
        MvcResult login = login(email, PASSWORD);
        Cookie first = refreshCookie(login);
        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andReturn();
        Cookie second = refreshCookie(refreshed);

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(second))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailIsNormalizedAndDuplicateRegistrationConflicts() throws Exception {
        register("Ana@Example.com", PASSWORD, null);
        assertThat(mail.lastMessage().to()).isEqualTo("ana@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("ana@example.com", PASSWORD, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void unconfirmedAccountCanLogInButSeesUnconfirmedStatus() throws Exception {
        register("bia@example.com", PASSWORD, "Bia");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("bia@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.emailConfirmed").value(false));
    }

    @Test
    void badCredentialsAreGeneric() throws Exception {
        register("caio@example.com", PASSWORD, null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("caio@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void invalidBodyAndUnknownFieldsAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("ana@example.com", "short", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"copacabana-2026","extra":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void confirmationTokenIsSingleUse() throws Exception {
        register("duda@example.com", PASSWORD, null);
        String token = confirmationToken();
        confirm(token);

        mockMvc.perform(post("/api/v1/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    void passwordResetRevokesSessionsAndChangesPassword() throws Exception {
        String email = "eva@example.com";
        register(email, PASSWORD, "Eva");
        confirm(confirmationToken());
        Cookie oldSession = refreshCookie(login(email, PASSWORD));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isAccepted());

        assertThat(mail.messages()).hasSize(2);
        String resetToken = resetToken();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"%s"}
                                """.formatted(resetToken, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(oldSession))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, NEW_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPasswordNeverRevealsWhetherAnAccountExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ghost@example.com"}
                                """))
                .andExpect(status().isAccepted());
        assertThat(mail.messages()).isEmpty();
    }

    @Test
    void refreshRejectsRequestsFromAnotherOrigin() throws Exception {
        register("fabio@example.com", PASSWORD, null);
        confirm(confirmationToken());
        Cookie cookie = refreshCookie(login("fabio@example.com", PASSWORD));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(cookie)
                        .header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(cookie)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk());
    }

    @Test
    void meIsProtectedAndUnknownRoutesUseProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void logoutClearsTheCookie() throws Exception {
        register("gabi@example.com", PASSWORD, null);
        confirm(confirmationToken());
        MvcResult login = login("gabi@example.com", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie(login)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    private void register(String email, String password, String displayName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, password, displayName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.user.emailConfirmed").value(false));
    }

    private void confirm(String token) throws Exception {
        mockMvc.perform(post("/api/v1/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(token)))
                .andExpect(status().isNoContent());
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase()))
                .andReturn();
    }

    private String accessToken(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private static Cookie refreshCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie header").isNotNull();
        String pair = setCookie.split(";", 2)[0];
        String[] parts = pair.split("=", 2);
        return new Cookie(parts[0], parts[1]);
    }

    private String confirmationToken() {
        Matcher matcher = CONFIRMATION_TOKEN.matcher(mail.lastMessage().text());
        assertThat(matcher.find()).as("confirmation link in email").isTrue();
        return matcher.group(1);
    }

    private String resetToken() {
        Matcher matcher = RESET_TOKEN.matcher(mail.lastMessage().text());
        assertThat(matcher.find()).as("reset link in email").isTrue();
        return matcher.group(1);
    }

    private String registerBody(String email, String password, String displayName) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("email", email);
        body.put("password", password);
        if (displayName != null) {
            body.put("displayName", displayName);
        }
        return objectMapper.writeValueAsString(body);
    }

    private String loginBody(String email, String password) {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }
}
