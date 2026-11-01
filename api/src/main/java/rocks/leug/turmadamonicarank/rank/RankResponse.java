package rocks.leug.turmadamonicarank.rank;

import java.time.Instant;
import java.util.List;

import rocks.leug.turmadamonicarank.common.SnapshotSummaryResponse;

public record RankResponse(
        Instant generatedAt,
        Instant nextCutoffAt,
        int totalBallots,
        SnapshotSummaryResponse latestSnapshot,
        List<RankEntryResponse> items,
        int limit,
        int offset,
        long total) {
}
