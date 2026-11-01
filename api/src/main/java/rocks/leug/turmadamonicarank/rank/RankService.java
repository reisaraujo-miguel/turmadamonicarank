package rocks.leug.turmadamonicarank.rank;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class RankService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final RankCalculator calculator;
    private final Clock clock;
    private volatile CachedRanking cached;

    public RankService(RankCalculator calculator, Clock clock) {
        this.calculator = calculator;
        this.clock = clock;
    }

    public Ranking currentRanking() {
        Instant now = clock.instant();
        CachedRanking current = cached;
        if (current != null && now.isBefore(current.expiresAt())) {
            return current.ranking();
        }
        synchronized (this) {
            current = cached;
            if (current != null && now.isBefore(current.expiresAt())) {
                return current.ranking();
            }
            Ranking ranking = calculator.calculate(null);
            cached = new CachedRanking(ranking, now.plus(CACHE_TTL));
            return ranking;
        }
    }

    public OptionalInt currentRank(UUID characterId) {
        var characters = currentRanking().characters();
        for (int index = 0; index < characters.size(); index++) {
            if (characters.get(index).id().equals(characterId)) {
                return OptionalInt.of(index + 1);
            }
        }
        return OptionalInt.empty();
    }

    public void invalidate() {
        cached = null;
    }

    private record CachedRanking(Ranking ranking, Instant expiresAt) {
    }
}
