package ReleaseBack.Back.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ReleaseBack.Back.DTO.MarketSignalDTO.Input;
import ReleaseBack.Back.DTO.MarketSignalDTO.Point;
import ReleaseBack.Back.DTO.MarketSignalDTO.Prediction;
import ReleaseBack.Back.entity.MarketObservation;

@Component
public class LinearMarketPredictionModel implements MarketPredictionModel {
    @Override
    public String cacheVersion() { return "linear-mock-v1"; }

    @Override
    public Prediction predict(List<MarketObservation> observations, Instant now) {
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        Map<String, List<MarketObservation>> series = new HashMap<>();
        for (MarketObservation observation : observations) {
            // A backfilled report is only usable after this system actually received it.
            if (observation.getDate() == null || observation.getDate().isAfter(today)
                    || observation.getValue() == null || !Double.isFinite(observation.getValue())
                    || (observation.getAvailableAt() != null && observation.getAvailableAt() > now.toEpochMilli())) continue;
            series.computeIfAbsent(observation.getIndicator(), key -> new ArrayList<>()).add(observation);
        }
        series.values().forEach(values -> values.sort(Comparator.comparing(MarketObservation::getDate)));
        addDerived(series);
        List<Input> inputs = new ArrayList<>();
        double coverage = 0;
        double weighted = 0;
        for (var spec : MarketInputCatalog.SPECS) {
            List<MarketObservation> values = series.getOrDefault(spec.code(), List.of());
            MarketObservation latest = values.isEmpty() ? null : values.getLast();
            String status = latest == null ? "missing"
                : latest.getDate().isBefore(today.minusDays(spec.maxAgeDays())) ? "stale" : "available";
            Double normalized = latest == null || spec.weight() == 0 ? null
                : Math.max(-1, Math.min(1, (latest.getValue() - spec.center()) / spec.scale()));
            if (status.equals("available") && normalized != null) {
                coverage += spec.weight();
                weighted += spec.weight() * normalized;
            }
            List<Point> history = values.stream().skip(Math.max(0, values.size() - 60))
                .map(v -> new Point(v.getDate(), v.getValue())).toList();
            inputs.add(new Input(spec.code(), spec.label(), spec.unit(), spec.source(), spec.url(),
                latest == null ? null : latest.getDate(), latest == null ? null : latest.getValue(),
                latest == null ? null : latest.getFetchedAt(), latest == null ? null : latest.getAvailableAt(),
                status, spec.transform(), normalized, spec.weight(), 0, null, history));
        }
        // Reweight available inputs only. Coverage below 50% suppresses the mock score.
        final double availableWeight = coverage;
        List<Input> contributions = inputs.stream().map(input -> {
            boolean included = input.status().equals("available") && input.weight() > 0;
            double weight = included && availableWeight > 0 ? input.weight() / availableWeight : 0;
            return new Input(input.indicator(), input.label(), input.unit(), input.source(), input.sourceUrl(),
                input.date(), input.value(), input.fetchedAt(), input.availableAt(), input.status(), input.transform(),
                input.normalized(), input.weight(), weight, included ? 100 * weight * input.normalized() : null,
                input.history());
        }).toList();
        return new Prediction(cacheVersion(), "S&P 500", "未来5个交易日（未校准）", now.toEpochMilli(),
            coverage + 1e-9 < .5 ? null : 100 * weighted / coverage,
            Math.min(1, coverage), coverage + 1e-9 < .5 ? "insufficient_data" : "mock", contributions);
    }

    private void addDerived(Map<String, List<MarketObservation>> series) {
        MarketObservation bull = latest(series, "aaii_bull"), bear = latest(series, "aaii_bear");
        if (sameDate(bull, bear)) {
            add(series, "aaii_spread", bull.getValue() - bear.getValue(), List.of(bull, bear));
        }
        MarketObservation longs = latest(series, "cot_long"), shorts = latest(series, "cot_short"), oi = latest(series, "cot_oi");
        if (sameDate(longs, shorts) && sameDate(longs, oi) && oi.getValue() > 0) {
            add(series, "cot_net", 100 * (longs.getValue() - shorts.getValue()) / oi.getValue(), List.of(longs, shorts, oi));
        }
        List<MarketObservation> prices = series.getOrDefault("sp500", List.of());
        if (prices.size() >= 6) {
            MarketObservation current = prices.getLast(), previous = prices.get(prices.size() - 6);
            // Do not call six widely separated observations a five-session return.
            if (previous.getValue() > 0 && !previous.getDate().isBefore(current.getDate().minusDays(12))) {
                add(series, "momentum5", 100 * (current.getValue() / previous.getValue() - 1), List.of(current, previous));
            }
        }
    }

    private MarketObservation latest(Map<String, List<MarketObservation>> series, String code) {
        List<MarketObservation> values = series.getOrDefault(code, List.of());
        return values.isEmpty() ? null : values.getLast();
    }

    private boolean sameDate(MarketObservation first, MarketObservation second) {
        return first != null && second != null && first.getDate().equals(second.getDate());
    }

    private void add(Map<String, List<MarketObservation>> series, String code, double value, List<MarketObservation> dependencies) {
        if (!Double.isFinite(value)) return;
        MarketObservation reference = dependencies.getFirst();
        MarketObservation result = new MarketObservation();
        result.setIndicator(code);
        result.setDate(reference.getDate());
        result.setValue(value);
        result.setSource(reference.getSource());
        result.setFetchedAt(dependencies.stream().map(MarketObservation::getFetchedAt).filter(java.util.Objects::nonNull).max(Long::compare).orElse(null));
        result.setAvailableAt(dependencies.stream().map(MarketObservation::getAvailableAt).filter(java.util.Objects::nonNull).max(Long::compare).orElse(null));
        series.put(code, List.of(result));
    }
}
