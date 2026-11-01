package rocks.leug.turmadamonicarank.character;

import java.util.UUID;

public record CharacterSummary(
        UUID id,
        String slug,
        String name,
        String imageUrl,
        String imageAlt,
        CharacterStatus status) {
}
