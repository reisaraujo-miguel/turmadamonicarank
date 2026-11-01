package rocks.leug.turmadamonicarank.character;

import java.util.List;
import java.util.UUID;

public record CharacterDetail(
        UUID id,
        String slug,
        String name,
        String fullName,
        String bio,
        String imageUrl,
        String imageAlt,
        String imageCredit,
        String sourceUrl,
        FirstAppearance firstAppearance,
        List<String> aliases,
        CharacterStatus status,
        Integer currentRank) {
}
