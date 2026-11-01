package rocks.leug.turmadamonicarank.rank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Ranking(Instant computedAt, int totalBallots, List<RankedCharacter> characters) {

    public record RankedCharacter(
            UUID id,
            String slug,
            String name,
            String imageUrl,
            String imageAlt,
            long points,
            int firstPlaceVotes,
            int ballotCount) {
    }
}
