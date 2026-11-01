package rocks.leug.turmadamonicarank.ballot;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record BallotRequest(@NotNull List<@NotNull UUID> characterIds) {
}
