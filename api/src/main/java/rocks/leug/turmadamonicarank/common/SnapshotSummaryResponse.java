package rocks.leug.turmadamonicarank.common;

import java.time.Instant;

public record SnapshotSummaryResponse(
        int year,
        int month,
        Instant cutoffAt,
        Instant generatedAt,
        int totalBallots,
        int totalCharacters) {
}
