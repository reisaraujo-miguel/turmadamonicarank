package rocks.leug.turmadamonicarank.character;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.common.ApiFieldError;
import rocks.leug.turmadamonicarank.common.PageResponse;
import rocks.leug.turmadamonicarank.rank.RankService;

@Service
public class CharacterDirectory {

    private static final String SUMMARY_SELECT = """
            SELECT c.id, c.slug, c.name, c.image_url, c.image_alt, c.status
            FROM characters c
            """;

    private final JdbcClient jdbcClient;
    private final RankService rankService;

    public CharacterDirectory(JdbcClient jdbcClient, RankService rankService) {
        this.jdbcClient = jdbcClient;
        this.rankService = rankService;
    }

    public PageResponse<CharacterSummary> search(String rawQuery, int limit, int offset) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (!query.isEmpty() && (query.length() < 3 || query.length() > 100)) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "The search query is invalid.",
                    List.of(new ApiFieldError(
                            "q",
                            ApiErrorCode.VALIDATION_FAILED.name(),
                            "must have between 3 and 100 characters")));
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        String where = "";
        String order;
        if (query.isEmpty()) {
            order = " ORDER BY lower(immutable_unaccent(c.name)) ASC, c.slug ASC";
        } else {
            String normalized = query.toLowerCase(Locale.ROOT);
            parameters.put("like", "%" + normalized + "%");
            parameters.put("prefix", normalized + "%");
            parameters.put("normalized", normalized);
            where = """
                    WHERE lower(immutable_unaccent(c.name)) LIKE :like
                       OR lower(immutable_unaccent(coalesce(c.full_name, ''))) LIKE :like
                       OR EXISTS (
                           SELECT 1
                           FROM character_aliases a
                           WHERE a.character_id = c.id
                             AND lower(immutable_unaccent(a.alias)) LIKE :like
                       )
                       OR similarity(lower(immutable_unaccent(c.name)), :normalized) >= 0.3
                    """;
            order = """
                     ORDER BY CASE
                         WHEN lower(immutable_unaccent(c.name)) = :normalized THEN 0
                         WHEN lower(immutable_unaccent(c.name)) LIKE :prefix THEN 1
                         WHEN lower(immutable_unaccent(coalesce(c.full_name, ''))) LIKE :prefix THEN 2
                         WHEN EXISTS (
                             SELECT 1
                             FROM character_aliases a
                             WHERE a.character_id = c.id
                               AND lower(immutable_unaccent(a.alias)) LIKE :prefix
                         ) THEN 3
                         ELSE 4
                     END,
                     similarity(lower(immutable_unaccent(c.name)), :normalized) DESC,
                     lower(immutable_unaccent(c.name)) ASC,
                     c.slug ASC
                    """;
        }

        JdbcClient.StatementSpec listSpec = jdbcClient
                .sql(SUMMARY_SELECT + where + order + " LIMIT :limit OFFSET :offset");
        JdbcClient.StatementSpec countSpec = jdbcClient
                .sql("SELECT count(*) FROM characters c " + where);
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            listSpec = listSpec.param(parameter.getKey(), parameter.getValue());
            countSpec = countSpec.param(parameter.getKey(), parameter.getValue());
        }
        List<CharacterSummary> items = listSpec
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapSummary)
                .list();
        long total = countSpec.query((resultSet, rowNumber) -> resultSet.getLong(1)).single();
        return new PageResponse<>(items, limit, offset, total);
    }

    public Optional<CharacterSummary> findById(UUID characterId) {
        return jdbcClient
                .sql(SUMMARY_SELECT + " WHERE c.id = :id")
                .param("id", characterId)
                .query(this::mapSummary)
                .optional();
    }

    public CharacterDetail detail(String slug) {
        CharacterRow row = jdbcClient
                .sql("""
                        SELECT c.id, c.slug, c.name, c.full_name, c.bio,
                               c.image_url, c.image_alt, c.image_credit, c.source_url,
                               c.first_appearance_year, c.first_appearance_work, c.status
                        FROM characters c
                        WHERE c.slug = :slug
                        """)
                .param("slug", slug)
                .query((resultSet, rowNumber) -> new CharacterRow(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("slug"),
                        resultSet.getString("name"),
                        resultSet.getString("full_name"),
                        resultSet.getString("bio"),
                        resultSet.getString("image_url"),
                        resultSet.getString("image_alt"),
                        resultSet.getString("image_credit"),
                        resultSet.getString("source_url"),
                        integerOrNull(resultSet.getObject("first_appearance_year")),
                        resultSet.getString("first_appearance_work"),
                        CharacterStatus.valueOf(resultSet.getString("status"))))
                .optional()
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.CHARACTER_NOT_FOUND, "The character was not found."));

        List<String> aliases = jdbcClient
                .sql("""
                        SELECT alias
                        FROM character_aliases
                        WHERE character_id = :characterId
                        ORDER BY lower(immutable_unaccent(alias)), alias
                        """)
                .param("characterId", row.id())
                .query((resultSet, rowNumber) -> resultSet.getString("alias"))
                .list();

        OptionalInt rank = rankService.currentRank(row.id());
        Integer currentRank = row.status() == CharacterStatus.INACTIVE || rank.isEmpty()
                ? null
                : rank.getAsInt();

        FirstAppearance firstAppearance = row.firstAppearanceYear() == null
                        && row.firstAppearanceWork() == null
                ? null
                : new FirstAppearance(row.firstAppearanceYear(), row.firstAppearanceWork());

        return new CharacterDetail(
                row.id(),
                row.slug(),
                row.name(),
                row.fullName(),
                row.bio(),
                row.imageUrl(),
                row.imageAlt(),
                row.imageCredit(),
                row.sourceUrl(),
                firstAppearance,
                aliases,
                row.status(),
                currentRank);
    }

    private CharacterSummary mapSummary(java.sql.ResultSet resultSet, int rowNumber)
            throws java.sql.SQLException {
        return new CharacterSummary(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("slug"),
                resultSet.getString("name"),
                resultSet.getString("image_url"),
                resultSet.getString("image_alt"),
                CharacterStatus.valueOf(resultSet.getString("status")));
    }

    private static Integer integerOrNull(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private record CharacterRow(
            UUID id,
            String slug,
            String name,
            String fullName,
            String bio,
            String imageUrl,
            String imageAlt,
            String imageCredit,
            String sourceUrl,
            Integer firstAppearanceYear,
            String firstAppearanceWork,
            CharacterStatus status) {
    }
}
