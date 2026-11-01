package rocks.leug.turmadamonicarank.rank;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class RankCalculator {

    private final JdbcClient jdbcClient;
    private final Clock clock;

    public RankCalculator(JdbcClient jdbcClient, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.clock = clock;
    }

    /**
     * Calculates the full ordered ranking. A null cutoff means the live rank;
     * otherwise only ballots last updated at or before the cutoff are counted.
     */
    public Ranking calculate(Instant cutoff) {
        String dateFilter = cutoff == null ? "" : " AND b.updated_at <= :cutoff";

        String rankingSql = """
                SELECT c.id,
                       c.slug,
                       c.name,
                       c.image_url,
                       c.image_alt,
                       SUM(11 - e.position)                   AS points,
                       COUNT(*) FILTER (WHERE e.position = 1) AS first_place_votes,
                       COUNT(*)                               AS ballot_count
                FROM ballot_entries e
                JOIN ballots b ON b.id = e.ballot_id
                JOIN users u ON u.id = b.user_id AND u.status = 'ACTIVE'
                JOIN characters c ON c.id = e.character_id AND c.status = 'ACTIVE'
                WHERE 1 = 1
                """ + dateFilter + """
                GROUP BY c.id, c.slug, c.name, c.image_url, c.image_alt
                HAVING SUM(11 - e.position) > 0
                ORDER BY points DESC, first_place_votes DESC, ballot_count DESC, c.slug ASC
                """;

        JdbcClient.StatementSpec rankingSpec = jdbcClient.sql(rankingSql);
        if (cutoff != null) {
            rankingSpec = rankingSpec.param("cutoff", OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC));
        }
        List<Ranking.RankedCharacter> characters = rankingSpec
                .query((resultSet, rowNumber) -> new Ranking.RankedCharacter(
                        resultSet.getObject("id", java.util.UUID.class),
                        resultSet.getString("slug"),
                        resultSet.getString("name"),
                        resultSet.getString("image_url"),
                        resultSet.getString("image_alt"),
                        resultSet.getLong("points"),
                        resultSet.getInt("first_place_votes"),
                        resultSet.getInt("ballot_count")))
                .list();

        String ballotsSql = """
                SELECT count(*)
                FROM ballots b
                JOIN users u ON u.id = b.user_id AND u.status = 'ACTIVE'
                WHERE 1 = 1
                """ + dateFilter;
        JdbcClient.StatementSpec ballotsSpec = jdbcClient.sql(ballotsSql);
        if (cutoff != null) {
            ballotsSpec = ballotsSpec.param("cutoff", OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC));
        }
        long totalBallots = ballotsSpec.query((resultSet, rowNumber) -> resultSet.getLong(1)).single();

        return new Ranking(clock.instant(), Math.toIntExact(totalBallots), characters);
    }
}
