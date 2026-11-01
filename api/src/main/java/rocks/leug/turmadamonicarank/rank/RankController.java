package rocks.leug.turmadamonicarank.rank;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import rocks.leug.turmadamonicarank.character.CharacterStatus;
import rocks.leug.turmadamonicarank.character.CharacterSummary;
import rocks.leug.turmadamonicarank.common.ApiErrorCode;
import rocks.leug.turmadamonicarank.common.ApiException;
import rocks.leug.turmadamonicarank.common.ApiFieldError;
import rocks.leug.turmadamonicarank.common.SnapshotPeriods;

@RestController
@RequestMapping("/api/v1/rank")
public class RankController {

    private final RankService rankService;
    private final SnapshotPeriods snapshotPeriods;
    private final Clock clock;

    public RankController(RankService rankService, SnapshotPeriods snapshotPeriods, Clock clock) {
        this.rankService = rankService;
        this.snapshotPeriods = snapshotPeriods;
        this.clock = clock;
    }

    @GetMapping
    public ResponseEntity<RankResponse> current(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        validatePage(limit, offset);
        Ranking ranking = rankService.currentRanking();
        int total = ranking.characters().size();
        List<Ranking.RankedCharacter> page = ranking.characters().subList(
                Math.min(offset, total), Math.min(offset + limit, total));
        List<RankEntryResponse> items = java.util.stream.IntStream.range(0, page.size())
                .mapToObj(index -> toResponse(page.get(index), offset + index + 1))
                .toList();

        RankResponse response = new RankResponse(
                ranking.computedAt(),
                snapshotPeriods.nextCutoff(clock.instant()),
                ranking.totalBallots(),
                null,
                items,
                limit,
                offset,
                total);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(response);
    }

    private static RankEntryResponse toResponse(Ranking.RankedCharacter character, int rank) {
        CharacterSummary summary = new CharacterSummary(
                character.id(),
                character.slug(),
                character.name(),
                character.imageUrl(),
                character.imageAlt(),
                CharacterStatus.ACTIVE);
        return new RankEntryResponse(
                rank,
                summary,
                character.points(),
                character.firstPlaceVotes(),
                character.ballotCount());
    }

    private static void validatePage(int limit, int offset) {
        if (limit < 1 || limit > 100) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "The page size is invalid.",
                    List.of(new ApiFieldError(
                            "limit",
                            ApiErrorCode.VALIDATION_FAILED.name(),
                            "must be between 1 and 100")));
        }
        if (offset < 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "The page offset is invalid.",
                    List.of(new ApiFieldError(
                            "offset",
                            ApiErrorCode.VALIDATION_FAILED.name(),
                            "must be zero or greater")));
        }
    }
}
