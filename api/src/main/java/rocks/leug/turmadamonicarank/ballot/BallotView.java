package rocks.leug.turmadamonicarank.ballot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import rocks.leug.turmadamonicarank.character.CharacterSummary;

public record BallotView(UUID id, Instant updatedAt, List<Entry> entries) {

    public record Entry(int position, CharacterSummary character) {
    }
}
