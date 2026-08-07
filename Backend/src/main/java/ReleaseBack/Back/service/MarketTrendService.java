package ReleaseBack.Back.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ReleaseBack.Back.DTO.MarketCorrelationDTO;
import ReleaseBack.Back.DTO.MarketTrendDTO;
import ReleaseBack.Back.DTO.MarketTrendPointDTO;
import ReleaseBack.Back.DTO.MarketTrendStockPointDTO;
import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.entity.UsStockIndexRecord;
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

    public MarketTrendDTO getLastSevenDays() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);

        Map<LocalDate, List<Double>> sentimentByDate = new LinkedHashMap<>();
        addSentiments(sentimentByDate, sentimentMapper.findSince(POLITICS_TABLE, fromDate));
        addSentiments(sentimentByDate, sentimentMapper.findSince(TECH_TABLE, fromDate));

        Map<LocalDate, List<UsStockIndexRecord>> stocksByDate = new LinkedHashMap<>();
        for (UsStockIndexRecord stock : safeList(stockIndexMapper.findSince(fromDate))) {
            if (stock != null && stock.getDate() != null) {
                stocksByDate.computeIfAbsent(stock.getDate(), ignored -> new ArrayList<>()).add(stock);
            }
        }

        List<MarketTrendPointDTO> points = new ArrayList<>();
        LocalDate date = fromDate;
        while (!date.isAfter(today)) {
            List<Double> sentiments = sentimentByDate.getOrDefault(date, Collections.emptyList());
            Double sentiment = sentiments.isEmpty() ? null : average(sentiments);
            List<MarketTrendStockPointDTO> stocks = new ArrayList<>();
            for (UsStockIndexRecord stock : stocksByDate.getOrDefault(date, Collections.emptyList())) {
                if (stock.getSymbol() != null && stock.getPrice() != null
                        && Double.isFinite(stock.getPrice())) {
                    stocks.add(new MarketTrendStockPointDTO(
                            stock.getSymbol(), stock.getName(), stock.getPrice()));
                }
            }
            points.add(new MarketTrendPointDTO(date, sentiment, stocks));
            date = date.plusDays(1);
        }

        return new MarketTrendDTO(points, calculateCorrelations(points));
    }

    private void addSentiments(
            Map<LocalDate, List<Double>> sentimentByDate,
            List<SentimentAverage> averages) {
        for (SentimentAverage average : safeList(averages)) {
            if (average != null && average.getDate() != null && average.getSentimentScore() != null
                    && Double.isFinite(average.getSentimentScore())) {
                sentimentByDate
                        .computeIfAbsent(average.getDate(), ignored -> new ArrayList<>())
                        .add(average.getSentimentScore());
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
                if (stock.getSymbol() == null || stock.getPrice() == null
                        || !Double.isFinite(stock.getPrice())) {
                    continue;
                }
                observationsBySymbol
                        .computeIfAbsent(stock.getSymbol(), ignored -> new ArrayList<>())
                        .add(new Observation(point.getSentiment(), stock.getPrice()));
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

    private Double correlation(List<Observation> observations) {
        if (observations.size() < 2) {
            return null;
        }

        double sentimentMean = observations.stream()
                .mapToDouble(Observation::sentiment)
                .average()
                .orElse(Double.NaN);
        double stockMean = observations.stream()
                .mapToDouble(Observation::stockPrice)
                .average()
                .orElse(Double.NaN);

        double covariance = 0;
        double sentimentVariance = 0;
        double stockVariance = 0;
        for (Observation observation : observations) {
            double sentimentDifference = observation.sentiment() - sentimentMean;
            double stockDifference = observation.stockPrice() - stockMean;
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
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private record Observation(double sentiment, double stockPrice) {
    }
}
