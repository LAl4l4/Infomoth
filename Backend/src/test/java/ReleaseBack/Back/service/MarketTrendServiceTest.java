package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ReleaseBack.Back.DTO.MarketCorrelationDTO;
import ReleaseBack.Back.DTO.MarketTrendDTO;
import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.entity.UsStockIndexRecord;
import ReleaseBack.Back.mapper.SentimentMapper;
import ReleaseBack.Back.mapper.UsStockIndexMapper;

@ExtendWith(MockitoExtension.class)
class MarketTrendServiceTest {

    @Mock
    private SentimentMapper sentimentMapper;

    @Mock
    private UsStockIndexMapper stockIndexMapper;

    @InjectMocks
    private MarketTrendService marketTrendService;

    @Test
    void buildsSevenCalendarDaysAndCalculatesPearsonCorrelation() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);
        when(sentimentMapper.findSince("politics_average", fromDate)).thenReturn(List.of(
                sentiment(fromDate.plusDays(4), 1.0),
                sentiment(fromDate.plusDays(5), 2.0),
                sentiment(today, 3.0)));
        when(sentimentMapper.findSince("tech_average", fromDate)).thenReturn(List.of());
        when(stockIndexMapper.findSince(fromDate)).thenReturn(List.of(
                stock(fromDate.plusDays(4), 1.0),
                stock(fromDate.plusDays(5), 2.0),
                stock(today, 3.0)));

        MarketTrendDTO result = marketTrendService.getLastSevenDays();

        assertEquals(7, result.getPoints().size());
        assertEquals(fromDate, result.getPoints().get(0).getDate());
        assertNull(result.getPoints().get(0).getSentiment());
        MarketCorrelationDTO correlation = result.getCorrelations().get(0);
        assertEquals("^GSPC", correlation.getSymbol());
        assertEquals(1.0, correlation.getCorr(), 0.000001);
        assertEquals(3, correlation.getSampleSize());
        assertEquals(104.0, result.getPoints().get(4).getStocks().get(0).getPrice());
        assertEquals(1.0, result.getPoints().get(4).getStocks().get(0).getChangePercent());
    }

    @Test
    void calculatesCorrelationFromAllAvailableNonConsecutiveDays() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);
        when(sentimentMapper.findSince("politics_average", fromDate)).thenReturn(List.of(
                sentiment(fromDate, 0.0),
                sentiment(fromDate.plusDays(1), 1.0),
                sentiment(fromDate.plusDays(2), 2.0),
                sentiment(fromDate.plusDays(4), 3.0),
                sentiment(fromDate.plusDays(5), 4.0),
                sentiment(fromDate.plusDays(6), 5.0)));
        when(sentimentMapper.findSince("tech_average", fromDate)).thenReturn(List.of());
        when(stockIndexMapper.findSince(fromDate)).thenReturn(List.of(
                stock(fromDate, 0.0),
                stock(fromDate.plusDays(1), 1.0),
                stock(fromDate.plusDays(2), 2.0),
                stock(fromDate.plusDays(4), 3.0),
                stock(fromDate.plusDays(5), 4.0),
                stock(fromDate.plusDays(6), 5.0)));

        MarketCorrelationDTO correlation = marketTrendService.getLastSevenDays()
                .getCorrelations()
                .get(0);

        assertEquals(6, correlation.getSampleSize());
        assertEquals(1.0, correlation.getCorr(), 0.000001);
    }

    private SentimentAverage sentiment(LocalDate date, double score) {
        SentimentAverage average = new SentimentAverage();
        average.setDate(date);
        average.setSentimentScore(score);
        return average;
    }

    private UsStockIndexRecord stock(LocalDate date, double changePercent) {
        UsStockIndexRecord stock = new UsStockIndexRecord();
        stock.setDate(date);
        stock.setSp500Price(100.0 + changePercent * 4);
        stock.setSp500ChangePercent(changePercent);
        stock.setDowJonesPrice(200.0 + changePercent * 4);
        stock.setDowJonesChangePercent(changePercent);
        stock.setNasdaqPrice(300.0 + changePercent * 4);
        stock.setNasdaqChangePercent(changePercent);
        stock.setRussell2000Price(400.0 + changePercent * 4);
        stock.setRussell2000ChangePercent(changePercent);
        return stock;
    }
}
