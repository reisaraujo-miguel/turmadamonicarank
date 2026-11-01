package rocks.leug.turmadamonicarank.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import rocks.leug.turmadamonicarank.config.RankProperties;

class SnapshotPeriodsTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private final SnapshotPeriods periods = new SnapshotPeriods(new RankProperties(SAO_PAULO));

    @Test
    void cutoffIsTheLastMomentOfThePeriodInSaoPaulo() {
        assertThat(periods.cutoff(YearMonth.of(2026, 11)))
                .isEqualTo(Instant.parse("2026-12-01T02:59:59.999Z"));
        assertThat(periods.cutoff(YearMonth.of(2028, 2)))
                .isEqualTo(Instant.parse("2028-03-01T02:59:59.999Z"));
    }

    @Test
    void currentPeriodRespectsTheLocalMonthBoundary() {
        assertThat(periods.currentPeriod(Instant.parse("2026-12-01T02:59:59.999Z")))
                .isEqualTo(YearMonth.of(2026, 11));
        assertThat(periods.currentPeriod(Instant.parse("2026-12-01T03:00:00Z")))
                .isEqualTo(YearMonth.of(2026, 12));
    }

    @Test
    void nextCutoffIsTheEndOfTheCurrentlyOpenPeriod() {
        assertThat(periods.nextCutoff(Instant.parse("2026-11-15T12:00:00Z")))
                .isEqualTo(Instant.parse("2026-12-01T02:59:59.999Z"));
        assertThat(periods.nextCutoff(Instant.parse("2026-12-01T03:00:00Z")))
                .isEqualTo(Instant.parse("2027-01-01T02:59:59.999Z"));
    }

    @Test
    void yearBoundariesUseTheSameLocalClock() {
        assertThat(periods.yearCutoff(Year.of(2026)))
                .isEqualTo(Instant.parse("2027-01-01T02:59:59.999Z"));
        assertThat(periods.currentYear(Instant.parse("2027-01-01T02:59:59.999Z")))
                .isEqualTo(Year.of(2026));
        assertThat(periods.currentYear(Instant.parse("2027-01-01T03:00:00Z")))
                .isEqualTo(Year.of(2027));
    }

    @Test
    void lastClosedPeriodIsThePreviousLocalMonth() {
        assertThat(periods.lastClosedPeriod(Instant.parse("2026-12-01T03:00:00Z")))
                .isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    void zoneIsRequired() {
        assertThatThrownBy(() -> new RankProperties(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("app.rank.zone");
    }
}
