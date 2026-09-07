package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ReleaseBack.Back.entity.MarketObservation;

class LinearMarketPredictionModelTest {
    private final Instant now = Instant.parse("2026-09-07T12:00:00Z");
    private final LinearMarketPredictionModel model = new LinearMarketPredictionModel();

    @Test
    void returnsNoPredictionForMissingOrStaleInputs() {
        var result = model.predict(List.of(point("vix", 10, "2026-08-01")), now);
        assertNull(result.score());
        assertEquals(0, result.coverage());
        assertEquals("insufficient_data", result.status());
        assertEquals("stale", result.inputs().stream().filter(i -> i.indicator().equals("vix")).findFirst().orElseThrow().status());
    }

    @Test
    void reweightsAvailableFeaturesAndContributionsSumToScore() {
        // All four available features saturate at +1; their weights total 50%.
        var result = model.predict(List.of(point("vix", 5, "2026-09-04"),
            point("tech_sentiment", 3, "2026-09-07"), point("politics_sentiment", 3, "2026-09-07"),
            point("hy_spread", 1, "2026-09-04"), point("treasury10y", 2, "2026-09-04")), now);
        assertEquals(.5, result.coverage(), 1e-9);
        assertEquals(100, result.score(), 1e-9);
        assertEquals(result.score(), result.inputs().stream().filter(i -> i.contribution() != null)
            .mapToDouble(i -> i.contribution()).sum(), 1e-9);
        assertEquals(1, result.inputs().stream().mapToDouble(i -> i.effectiveWeight()).sum(), 1e-9);
        assertNull(result.inputs().stream().filter(i -> i.indicator().equals("put_call")).findFirst().orElseThrow().contribution());
    }

    @Test
    void excludesFutureAvailabilityAndDoesNotCombineDifferentReportDates() {
        MarketObservation future = point("vix", 10, "2026-09-04");
        future.setAvailableAt(now.plusSeconds(1).toEpochMilli());
        var result = model.predict(List.of(future, point("cot_long", 100, "2026-09-01"),
            point("cot_short", 50, "2026-08-25"), point("cot_oi", 200, "2026-09-01")), now);
        assertEquals(0, result.coverage());
        assertEquals("missing", result.inputs().stream().filter(i -> i.indicator().equals("cot_net")).findFirst().orElseThrow().status());
    }

    @Test
    void derivesSameReportNetPositionAndFiveSessionMomentum() {
        List<MarketObservation> observations = new ArrayList<>(List.of(point("cot_long", 100, "2026-09-01"),
            point("cot_short", 50, "2026-09-01"), point("cot_oi", 200, "2026-09-01")));
        String[] dates = {"2026-08-28", "2026-08-31", "2026-09-01", "2026-09-02", "2026-09-03", "2026-09-04"};
        for (int i = 0; i < dates.length; i++) observations.add(point("sp500", 100 + i, dates[i]));
        var result = model.predict(observations, now);
        assertEquals(25, result.inputs().stream().filter(i -> i.indicator().equals("cot_net")).findFirst().orElseThrow().value());
        assertEquals(5, result.inputs().stream().filter(i -> i.indicator().equals("momentum5")).findFirst().orElseThrow().value(), 1e-9);
    }

    private MarketObservation point(String code, double value, String date) {
        MarketObservation result = new MarketObservation();
        result.setIndicator(code);
        result.setDate(LocalDate.parse(date));
        result.setValue(value);
        result.setSource("test");
        result.setAvailableAt(now.minusSeconds(1).toEpochMilli());
        return result;
    }
}
