package rocks.leug.turmadamonicarank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

class BallotAndRankApiIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "copacabana-2026";
    private static final Pattern CONFIRMATION_TOKEN =
            Pattern.compile("/confirmar-email\\?token=([A-Za-z0-9_-]+)");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void characterSearchIsAccentInsensitiveAndDetailIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/characters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/characters").param("q", "monica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("monica"));

        mockMvc.perform(get("/api/v1/characters").param("q", "cascao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("cascao"));

        mockMvc.perform(get("/api/v1/characters").param("q", "mon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("monica"));

        mockMvc.perform(get("/api/v1/characters").param("q", "mo"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("q"));

        mockMvc.perform(get("/api/v1/characters/monica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("monica"))
                .andExpect(jsonPath("$.bio").isString())
                .andExpect(jsonPath("$.aliases[0]").isString())
                .andExpect(jsonPath("$.aliases").value(org.hamcrest.Matchers.hasItem("Monica")))
                .andExpect(jsonPath("$.currentRank").value(nullValue()));

        mockMvc.perform(get("/api/v1/characters/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHARACTER_NOT_FOUND"));
    }

    @Test
    void ballotRequiresAuthenticationAndConfirmation() throws Exception {
        mockMvc.perform(get("/api/v1/ballot"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        String email = uniqueEmail();
        register(email);
        String token = loginWithoutConfirmation(email);

        mockMvc.perform(put("/api/v1/ballot")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ballotBody(ids("monica"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_EMAIL_NOT_CONFIRMED"));

        mockMvc.perform(get("/api/v1/ballot")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ballot").value(nullValue()));
    }

    @Test
    void ballotReplacementUpdatesTheLiveRankAndIsAtomic() throws Exception {
        String token = confirmedSession();
        List<UUID> initial = ids("monica", "cebolinha", "cascao");

        mockMvc.perform(get("/api/v1/ballot").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ballot").value(nullValue()));

        mockMvc.perform(put("/api/v1/ballot")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ballotBody(initial)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(3))
                .andExpect(jsonPath("$.entries[0].position").value(1))
                .andExpect(jsonPath("$.entries[0].character.slug").value("monica"));

        mockMvc.perform(get("/api/v1/characters/monica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRank").value(1));

        mockMvc.perform(get("/api/v1/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBallots").value(1))
                .andExpect(jsonPath("$.latestSnapshot").value(nullValue()))
                .andExpect(jsonPath("$.nextCutoffAt").isString())
                .andExpect(jsonPath("$.items[0].character.slug").value("monica"))
                .andExpect(jsonPath("$.items[0].points").value(10))
                .andExpect(jsonPath("$.items[1].points").value(9))
                .andExpect(jsonPath("$.items[2].points").value(8));

        mockMvc.perform(put("/api/v1/ballot")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ballotBody(ids("monica", "monica"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BALLOT_DUPLICATE_CHARACTER"));

        mockMvc.perform(get("/api/v1/ballot").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ballot.entries.length()").value(3));

        mockMvc.perform(put("/api/v1/ballot")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ballotBody(ids("magali", "monica"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].character.slug").value("magali"));

        mockMvc.perform(get("/api/v1/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBallots").value(1))
                .andExpect(jsonPath("$.items[0].character.slug").value("magali"))
                .andExpect(jsonPath("$.items[1].character.slug").value("monica"))
                .andExpect(jsonPath("$.items[1].points").value(9));

        mockMvc.perform(delete("/api/v1/ballot").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/ballot").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ballot").value(nullValue()));

        mockMvc.perform(get("/api/v1/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBallots").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void rankOrdersByPointsAndUsesStableTieBreaks() throws Exception {
        populate(confirmedSession(), "monica", "cebolinha");
        populate(confirmedSession(), "cebolinha", "monica");
        populate(confirmedSession(), "magali");

        mockMvc.perform(get("/api/v1/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBallots").value(3))
                .andExpect(jsonPath("$.items[0].character.slug").value("cebolinha"))
                .andExpect(jsonPath("$.items[0].points").value(19))
                .andExpect(jsonPath("$.items[1].character.slug").value("monica"))
                .andExpect(jsonPath("$.items[1].points").value(19))
                .andExpect(jsonPath("$.items[2].character.slug").value("magali"))
                .andExpect(jsonPath("$.items[2].points").value(10))
                .andExpect(jsonPath("$.items[0].firstPlaceVotes").value(1));
    }

    @Test
    void inactiveCharactersStayBrowsableButCannotBeVotedFor() throws Exception {
        String token = confirmedSession();
        populate(token, "cascao", "monica");

        jdbcTemplate.update(
                "UPDATE characters SET status = 'INACTIVE' WHERE slug = 'cascao'");
        rankService.invalidate();

        mockMvc.perform(get("/api/v1/rank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].character.slug").value("monica"))
                .andExpect(jsonPath("$.items[0].points").value(9))
                .andExpect(jsonPath("$.totalBallots").value(1));

        mockMvc.perform(get("/api/v1/characters/cascao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.currentRank").value(nullValue()));

        mockMvc.perform(get("/api/v1/characters").param("q", "cascao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("cascao"));

        mockMvc.perform(put("/api/v1/ballot")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ballotBody(ids("cascao"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BALLOT_CHARACTER_INACTIVE"))
                .andExpect(jsonPath("$.errors[0].code").value("BALLOT_CHARACTER_INACTIVE"));

        mockMvc.perform(put("/api/v1/ballot")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ballotBody(ids("monica"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1));
    }

    private void populate(String token, String... slugs) throws Exception {
        mockMvc.perform(put("/api/v1/ballot")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ballotBody(ids(slugs))))
                .andExpect(status().isOk());
    }

    private String confirmedSession() throws Exception {
        String email = uniqueEmail();
        register(email);
        mockMvc.perform(post("/api/v1/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(confirmationToken())))
                .andExpect(status().isNoContent());
        return loginWithoutConfirmation(email);
    }

    private void register(String email) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", PASSWORD);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private String loginWithoutConfirmation(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private String confirmationToken() {
        Matcher matcher = CONFIRMATION_TOKEN.matcher(mail.lastMessage().text());
        assertThat(matcher.find()).as("confirmation link in email").isTrue();
        return matcher.group(1);
    }

    private List<UUID> ids(String... slugs) {
        return Arrays.stream(slugs)
                .map(slug -> jdbcTemplate.queryForObject(
                        "SELECT id FROM characters WHERE slug = ?", UUID.class, slug))
                .toList();
    }

    private String ballotBody(List<UUID> characterIds) throws Exception {
        return objectMapper.writeValueAsString(Map.of("characterIds", characterIds));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "m2-" + UUID.randomUUID() + "@example.com";
    }
}
