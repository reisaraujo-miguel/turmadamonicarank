package rocks.leug.turmadamonicarank.rank;

import rocks.leug.turmadamonicarank.character.CharacterSummary;

public record RankEntryResponse(
        int rank,
        CharacterSummary character,
        long points,
        int firstPlaceVotes,
        int ballotCount) {
}
