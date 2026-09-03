package ReleaseBack.Back.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import ReleaseBack.Back.DTO.MarketCorrelationDTO;
import ReleaseBack.Back.DTO.MarketTrendDTO;
import ReleaseBack.Back.DTO.MarketTrendPointDTO;
import ReleaseBack.Back.DTO.MarketTrendStockPointDTO;
import ReleaseBack.Back.entity.MarketCorrelation;
import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.entity.UsStockIndexRecord;
import ReleaseBack.Back.mapper.MarketCorrelationMapper;
import ReleaseBack.Back.mapper.SentimentMapper;
import ReleaseBack.Back.mapper.UsStockIndexMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketTrendService {
    private static final String POLITICS_TABLE = "politics_average";
    private static final String TECH_TABLE = "tech_average";

    private final SentimentMapper sentimentMapper;
    private final UsStockIndexMapper stockIndexMapper;
    private final MarketCorrelationMapper correlationMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeCorrelations() {
        if (safeList(correlationMapper.findAll()).isEmpty()) {
            rollingUpdateCorrelation();
        }
    }

    // Recomputes Pearson correlations over completed persisted days and upserts
    // one row per symbol into market_correlation. The daily scheduler keeps
    // request-time reads independent from the size of the historical dataset.
    @Transactional
    public void rollingUpdateCorrelation() {
        Map<LocalDate, List<Double>> sentimentByDate = new LinkedHashMap<>();
        addSentiments(sentimentByDate, sentimentMapper.findAllByTable(POLITICS_TABLE));
        addSentiments(sentimentByDate, sentimentMapper.findAllByTable(TECH_TABLE));

        Map<LocalDate, UsStockIndexRecord> stocksByDate = new LinkedHashMap<>();
        for (UsStockIndexRecord stock : safeList(stockIndexMapper.findAll())) {
            if (stock != null && stock.getDate() != null) {
                stocksByDate.put(stock.getDate(), stock);
            }
        }

        List<MarketTrendPointDTO> points = buildPoints(
                sentimentByDate,
                stocksByDate,
                LocalDate.now().minusDays(1));
        for (MarketCorrelationDTO correlation : calculateCorrelations(points)) {
            MarketCorrelation record = new MarketCorrelation();
            record.setSymbol(correlation.getSymbol());
            record.setName(correlation.getName());
            record.setCorr(correlation.getCorr());
            record.setSampleSize(correlation.getSampleSize());
            if (correlationMapper.updateCorrelation(record) == 0) {
                try {
                    correlationMapper.insertCorrelation(record);
                } catch (DuplicateKeyException ignored) {
                    correlationMapper.updateCorrelation(record);
                }
            }
        }
    }

    public MarketTrendDTO getMarketTrends() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);

        Map<LocalDate, List<Double>> sentimentByDate = new LinkedHashMap<>();
        addSentiments(sentimentByDate, sentimentMapper.findSince(POLITICS_TABLE, fromDate));
        addSentiments(sentimentByDate, sentimentMapper.findSince(TECH_TABLE, fromDate));

        Map<LocalDate, UsStockIndexRecord> stocksByDate = new LinkedHashMap<>();
        for (UsStockIndexRecord stock : safeList(stockIndexMapper.findSince(fromDate))) {
            if (stock != null && stock.getDate() != null) {
                stocksByDate.put(stock.getDate(), stock);
            }
        }

        List<MarketTrendPointDTO> points = new ArrayList<>();
        LocalDate date = fromDate;
        while (!date.isAfter(today)) {
            List<Double> sentiments = sentimentByDate.getOrDefault(date, Collections.emptyList());
            Double sentiment = sentiments.isEmpty() ? null : average(sentiments);
            List<MarketTrendStockPointDTO> stocks = toStockPoints(stocksByDate.get(date));
            points.add(new MarketTrendPointDTO(date, sentiment, stocks));
            date = date.plusDays(1);
        }

        return new MarketTrendDTO(points, findPersistedCorrelations());
    }

    private List<MarketTrendPointDTO> buildPoints(
            Map<LocalDate, List<Double>> sentimentByDate,
            Map<LocalDate, UsStockIndexRecord> stocksByDate,
            LocalDate throughDate) {
        Set<LocalDate> dates = new TreeSet<>();
        dates.addAll(sentimentByDate.keySet());
        dates.addAll(stocksByDate.keySet());

        List<MarketTrendPointDTO> points = new ArrayList<>();
        for (LocalDate date : dates) {
            if (date.isAfter(throughDate)) {
                continue;
            }
            List<Double> sentiments = sentimentByDate.getOrDefault(date, Collections.emptyList());
            Double sentiment = sentiments.isEmpty() ? null : average(sentiments);
            points.add(new MarketTrendPointDTO(date, sentiment, toStockPoints(stocksByDate.get(date))));
        }
        return points;
    }

    private List<MarketCorrelationDTO> findPersistedCorrelations() {
        List<MarketCorrelationDTO> correlations = new ArrayList<>();
        for (MarketCorrelation record : safeList(correlationMapper.findAll())) {
            if (record == null || record.getSymbol() == null) {
                continue;
            }
            correlations.add(new MarketCorrelationDTO(
                    record.getSymbol(),
                    record.getName(),
                    record.getCorr(),
                    record.getSampleSize()));
        }
        return correlations;
    }

    private void addSentiments(
            Map<LocalDate, List<Double>> sentimentByDate,
            List<SentimentAverage> averages) {
        for (SentimentAverage average : safeList(averages)) {
            Double normalizedScore = SentimentScoreCalculator.normalizedScore(average);
            if (average != null && average.getDate() != null && normalizedScore != null
                    && Double.isFinite(normalizedScore)) {
                sentimentByDate
                        .computeIfAbsent(average.getDate(), ignored -> new ArrayList<>())
                        .add(normalizedScore);
            }
        }
    }

    private List<MarketCorrelationDTO> calculateCorrelations(List<MarketTrendPointDTO> points) {
        Map<String, List<Observation>> observationsBySymbol = new LinkedHashMap<>();
        Map<String, String> namesBySymbol = new LinkedHashMap<>();

        for (MarketTrendPointDTO point : points) {
            if (point.getSentiment() == null || !Double.isFinite(point.getSentiment())) {
                continue;
            }
            for (MarketTrendStockPointDTO stock : point.getStocks()) {
                if (stock.getSymbol() == null || stock.getChangePercent() == null
                        || !Double.isFinite(stock.getChangePercent())) {
                    continue;
                }
                observationsBySymbol
                        .computeIfAbsent(stock.getSymbol(), ignored -> new ArrayList<>())
                        .add(new Observation(point.getSentiment(), stock.getChangePercent()));
                namesBySymbol.putIfAbsent(stock.getSymbol(), stock.getName());
            }
        }

        List<MarketCorrelationDTO> correlations = new ArrayList<>();
        for (Map.Entry<String, List<Observation>> entry : observationsBySymbol.entrySet()) {
            List<Observation> observations = entry.getValue();
            correlations.add(new MarketCorrelationDTO(
                    entry.getKey(),
                    namesBySymbol.get(entry.getKey()),
                    correlation(observations),
                    observations.size()));
        }
        return correlations;
    }

    private List<MarketTrendStockPointDTO> toStockPoints(UsStockIndexRecord record) {
        if (record == null) {
            return Collections.emptyList();
        }

        List<MarketTrendStockPointDTO> stocks = new ArrayList<>();
        addStock(stocks, "^GSPC", "S&P 500", record.getSp500Price(), record.getSp500ChangePercent());
        addStock(
                stocks,
                "^DJI",
                "Dow Jones Industrial Average",
                record.getDowJonesPrice(),
                record.getDowJonesChangePercent());
        addStock(stocks, "^IXIC", "NASDAQ Composite", record.getNasdaqPrice(), record.getNasdaqChangePercent());
        addStock(
                stocks,
                "^RUT",
                "Russell 2000",
                record.getRussell2000Price(),
                record.getRussell2000ChangePercent());
        return stocks;
    }

    private void addStock(
            List<MarketTrendStockPointDTO> stocks,
            String symbol,
            String name,
            Double price,
            Double changePercent) {
        if (price != null && Double.isFinite(price)
                && changePercent != null && Double.isFinite(changePercent)) {
            stocks.add(new MarketTrendStockPointDTO(symbol, name, price, changePercent));
        }
    }

    private Double correlation(List<Observation> observations) {
        if (observations.size() < 2) {
            return null;
        }

        double sentimentMean = observations.stream()
                .mapToDouble(observation -> observation.sentiment())
                .average()
                .orElse(Double.NaN);
        double stockMean = observations.stream()
                .mapToDouble(observation -> observation.stockReturn())
                .average()
                .orElse(Double.NaN);

        double covariance = 0;
        double sentimentVariance = 0;
        double stockVariance = 0;
        for (Observation observation : observations) {
            double sentimentDifference = observation.sentiment() - sentimentMean;
            double stockDifference = observation.stockReturn() - stockMean;
            covariance += sentimentDifference * stockDifference;
            sentimentVariance += sentimentDifference * sentimentDifference;
            stockVariance += stockDifference * stockDifference;
        }

        double sampleSize = observations.size();
        covariance /= sampleSize;
        sentimentVariance /= sampleSize;
        stockVariance /= sampleSize;
        double sentimentStandardDeviation = Math.sqrt(sentimentVariance);
        double stockStandardDeviation = Math.sqrt(stockVariance);
        double denominator = sentimentStandardDeviation * stockStandardDeviation;
        if (denominator == 0 || !Double.isFinite(denominator)) {
            return null;
        }

        double corr = covariance / denominator;
        return Math.max(-1.0, Math.min(1.0, corr));
    }

    private double average(List<Double> values) {
        return values.stream().mapToDouble(value -> value.doubleValue()).average().orElse(0.0);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private record Observation(double sentiment, double stockReturn) {
    }
}
