package rocks.leug.turmadamonicarank.ballot;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rocks.leug.turmadamonicarank.character.CharacterDirectory;
import rocks.leug.turmadamonicarank.character.CharacterStatus;
import rocks.leug.turmadamonicarank.character.CharacterSummary;
import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.common.ApiFieldError;
import rocks.leug.turmadamonicarank.rank.RankService;
import rocks.leug.turmadamonicarank.user.User;
import rocks.leug.turmadamonicarank.user.UserRepository;
import rocks.leug.turmadamonicarank.user.UserStatus;

@Service
public class BallotService {

    private final JdbcClient jdbcClient;
    private final UserRepository userRepository;
    private final CharacterDirectory characterDirectory;
    private final RankService rankService;
    private final Clock clock;

    public BallotService(
            JdbcClient jdbcClient,
            UserRepository userRepository,
            CharacterDirectory characterDirectory,
            RankService rankService,
            Clock clock) {
        this.jdbcClient = jdbcClient;
        this.userRepository = userRepository;
        this.characterDirectory = characterDirectory;
        this.rankService = rankService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<BallotView> current(UUID userId) {
        Optional<BallotRow> ballot = jdbcClient
                .sql("SELECT id, updated_at FROM ballots WHERE user_id = :userId")
                .param("userId", userId)
                .query((resultSet, rowNumber) -> new BallotRow(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("updated_at", java.time.OffsetDateTime.class).toInstant()))
                .optional();
        if (ballot.isEmpty()) {
            return Optional.empty();
        }

        List<BallotView.Entry> entries = jdbcClient
                .sql("""
                        SELECT e.position, c.id, c.slug, c.name, c.image_url, c.image_alt, c.status
                        FROM ballot_entries e
                        JOIN characters c ON c.id = e.character_id
                        WHERE e.ballot_id = :ballotId
                        ORDER BY e.position
                        """)
                .param("ballotId", ballot.get().id())
                .query((resultSet, rowNumber) -> new BallotView.Entry(
                        resultSet.getInt("position"),
                        new CharacterSummary(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("slug"),
                                resultSet.getString("name"),
                                resultSet.getString("image_url"),
                                resultSet.getString("image_alt"),
                                CharacterStatus.valueOf(resultSet.getString("status")))))
                .list();
        return Optional.of(new BallotView(ballot.get().id(), ballot.get().updatedAt(), entries));
    }

    @Transactional
    public BallotView replace(UUID userId, List<UUID> characterIds) {
        requireConfirmedUser(userId);
        List<CharacterSummary> characters = validate(characterIds);

        Instant now = clock.instant();
        UUID ballotId = jdbcClient
                .sql("""
                        INSERT INTO ballots (user_id, updated_at)
                        VALUES (:userId, :now)
                        ON CONFLICT (user_id) DO UPDATE SET updated_at = EXCLUDED.updated_at
                        RETURNING id
                        """)
                .param("userId", userId)
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .query((resultSet, rowNumber) -> resultSet.getObject("id", UUID.class))
                .single();

        jdbcClient
                .sql("DELETE FROM ballot_entries WHERE ballot_id = :ballotId")
                .param("ballotId", ballotId)
                .update();
        for (int index = 0; index < characters.size(); index++) {
            jdbcClient
                    .sql("""
                            INSERT INTO ballot_entries (ballot_id, character_id, position)
                            VALUES (:ballotId, :characterId, :position)
                            """)
                    .param("ballotId", ballotId)
                    .param("characterId", characters.get(index).id())
                    .param("position", index + 1)
                    .update();
        }
        rankService.invalidate();
        invalidateRankAfterCommit();
        return current(userId).orElseThrow(() -> new IllegalStateException("Ballot was not saved"));
    }

    @Transactional
    public void withdraw(UUID userId) {
        requireConfirmedUser(userId);
        jdbcClient
                .sql("DELETE FROM ballots WHERE user_id = :userId")
                .param("userId", userId)
                .update();
        rankService.invalidate();
        invalidateRankAfterCommit();
    }

    private void invalidateRankAfterCommit() {
        if (org.springframework.transaction.support.TransactionSynchronizationManager
                .isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            rankService.invalidate();
                        }
                    });
        }
    }

    private void requireConfirmedUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.AUTH_REQUIRED, "Authentication is required."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "This account is disabled.");
        }
        if (!user.isEmailConfirmed()) {
            throw new ApiException(
                    ApiErrorCode.AUTH_EMAIL_NOT_CONFIRMED,
                    "Confirm your email before voting.");
        }
    }

    private List<CharacterSummary> validate(List<UUID> characterIds) {
        if (characterIds == null || characterIds.isEmpty() || characterIds.size() > 10) {
            throw new ApiException(
                    ApiErrorCode.BALLOT_INVALID_SIZE,
                    "A ballot must contain between 1 and 10 characters.",
                    List.of(new ApiFieldError(
                            "characterIds",
                            ApiErrorCode.BALLOT_INVALID_SIZE.name(),
                            "must contain between 1 and 10 characters")));
        }

        Set<UUID> seen = new LinkedHashSet<>();
        List<ApiFieldError> duplicateErrors = new ArrayList<>();
        for (int index = 0; index < characterIds.size(); index++) {
            UUID characterId = characterIds.get(index);
            if (!seen.add(characterId)) {
                duplicateErrors.add(new ApiFieldError(
                        "characterIds[" + index + "]",
                        ApiErrorCode.BALLOT_DUPLICATE_CHARACTER.name(),
                        "the same character cannot appear twice"));
            }
        }
        if (!duplicateErrors.isEmpty()) {
            throw new ApiException(
                    ApiErrorCode.BALLOT_DUPLICATE_CHARACTER,
                    "A character can appear only once on a ballot.",
                    duplicateErrors);
        }

        List<CharacterSummary> characters = new ArrayList<>();
        List<ApiFieldError> unknownErrors = new ArrayList<>();
        List<ApiFieldError> inactiveErrors = new ArrayList<>();
        for (int index = 0; index < characterIds.size(); index++) {
            UUID characterId = characterIds.get(index);
            Optional<CharacterSummary> character = characterDirectory.findById(characterId);
            if (character.isEmpty()) {
                unknownErrors.add(new ApiFieldError(
                        "characterIds[" + index + "]",
                        ApiErrorCode.BALLOT_CHARACTER_UNKNOWN.name(),
                        "character does not exist"));
                continue;
            }
            if (character.get().status() != CharacterStatus.ACTIVE) {
                inactiveErrors.add(new ApiFieldError(
                        "characterIds[" + index + "]",
                        ApiErrorCode.BALLOT_CHARACTER_INACTIVE.name(),
                        "character is not active"));
                continue;
            }
            characters.add(character.get());
        }
        if (!unknownErrors.isEmpty()) {
            throw new ApiException(
                    ApiErrorCode.BALLOT_CHARACTER_UNKNOWN,
                    "One or more characters do not exist.",
                    unknownErrors);
        }
        if (!inactiveErrors.isEmpty()) {
            throw new ApiException(
                    ApiErrorCode.BALLOT_CHARACTER_INACTIVE,
                    "One or more characters are not active.",
                    inactiveErrors);
        }
        return characters;
    }

    private record BallotRow(UUID id, Instant updatedAt) {
    }
}
