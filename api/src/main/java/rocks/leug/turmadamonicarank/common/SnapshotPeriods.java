package rocks.leug.turmadamonicarank.common;

import java.time.Instant;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import rocks.leug.turmadamonicarank.config.RankProperties;

/**
 * Single source of truth for ranking periods. All month and year boundaries are
 * computed in the configured audience zone, never in the server zone.
 */
@Component
public class SnapshotPeriods {

    private static final LocalTime LAST_MOMENT = LocalTime.of(23, 59, 59, 999_000_000);

    private final ZoneId zone;

    public SnapshotPeriods(RankProperties properties) {
        this.zone = properties.zone();
    }

    public ZoneId zone() {
        return zone;
    }

    public Instant cutoff(YearMonth period) {
        return period.atEndOfMonth().atTime(LAST_MOMENT).atZone(zone).toInstant();
    }

    public YearMonth currentPeriod(Instant instant) {
        return YearMonth.from(instant.atZone(zone));
    }

    public Instant nextCutoff(Instant instant) {
        return cutoff(currentPeriod(instant));
    }

    public Instant yearCutoff(Year year) {
        return year.atMonth(Month.DECEMBER).atDay(31).atTime(LAST_MOMENT).atZone(zone).toInstant();
    }

    public Year currentYear(Instant instant) {
        return Year.of(instant.atZone(zone).getYear());
    }

    public YearMonth lastClosedPeriod(Instant instant) {
        return currentPeriod(instant).minusMonths(1);
    }
}
