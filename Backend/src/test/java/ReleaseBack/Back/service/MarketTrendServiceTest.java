package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import ReleaseBack.Back.DTO.MarketCorrelationDTO;
import ReleaseBack.Back.DTO.MarketTrendDTO;
import ReleaseBack.Back.entity.MarketCorrelation;
import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.entity.UsStockIndexRecord;
import ReleaseBack.Back.mapper.MarketCorrelationMapper;
import ReleaseBack.Back.mapper.SentimentMapper;
import ReleaseBack.Back.mapper.UsStockIndexMapper;

@ExtendWith(MockitoExtension.class)
class MarketTrendServiceTest {

    @Mock
    private SentimentMapper sentimentMapper;

    @Mock
    private UsStockIndexMapper stockIndexMapper;

    @Mock
    private MarketCorrelationMapper correlationMapper;

    @InjectMocks
    private MarketTrendService marketTrendService;

    @Test
    void buildsSevenCalendarDaysAndReturnsPersistedCorrelations() {
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
        when(correlationMapper.findAll()).thenReturn(List.of(persisted("^GSPC", "S&P 500", 1.0, 3)));

        MarketTrendDTO result = marketTrendService.getMarketTrends();

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
    void rollingUpdatePersistsFullHistoryCorrelations() {
        LocalDate oldDate = LocalDate.now().minusMonths(1);
        when(sentimentMapper.findAllByTable("politics_average")).thenReturn(List.of(
                sentiment(oldDate, 0.0),
                sentiment(oldDate.plusDays(1), 1.0)));
        when(sentimentMapper.findAllByTable("tech_average")).thenReturn(List.of());
        when(stockIndexMapper.findAll()).thenReturn(List.of(
                stock(oldDate, 0.0),
                stock(oldDate.plusDays(1), 1.0)));
        when(correlationMapper.updateCorrelation(any())).thenReturn(1);

        marketTrendService.rollingUpdateCorrelation();

        ArgumentCaptor<MarketCorrelation> captor = ArgumentCaptor.forClass(MarketCorrelation.class);
        verify(correlationMapper, times(4)).updateCorrelation(captor.capture());
        verify(correlationMapper, never()).insertCorrelation(any());
        MarketCorrelation record = captor.getAllValues().stream()
                .filter(candidate -> "^DJI".equals(candidate.getSymbol()))
                .findFirst()
                .orElseThrow();
        assertEquals(1.0, record.getCorr(), 0.000001);
        assertEquals(2, record.getSampleSize());
    }

    @Test
    void rollingUpdateInsertsWhenNoRowExistsYet() {
        LocalDate oldDate = LocalDate.now().minusMonths(1);
        when(sentimentMapper.findAllByTable("politics_average")).thenReturn(List.of(
                sentiment(oldDate, 0.0),
                sentiment(oldDate.plusDays(1), 1.0)));
        when(sentimentMapper.findAllByTable("tech_average")).thenReturn(List.of());
        when(stockIndexMapper.findAll()).thenReturn(List.of(
                stock(oldDate, 0.0),
                stock(oldDate.plusDays(1), 1.0)));
        when(correlationMapper.updateCorrelation(any())).thenReturn(0);

        marketTrendService.rollingUpdateCorrelation();

        ArgumentCaptor<MarketCorrelation> captor = ArgumentCaptor.forClass(MarketCorrelation.class);
        verify(correlationMapper, times(4)).insertCorrelation(captor.capture());
        MarketCorrelation record = captor.getAllValues().stream()
                .filter(candidate -> "^RUT".equals(candidate.getSymbol()))
                .findFirst()
                .orElseThrow();
        assertEquals(1.0, record.getCorr(), 0.000001);
        assertEquals(2, record.getSampleSize());
    }

    @Test
    void rollingUpdateDefersTodayUntilItsDailyValuesAreFinal() {
        LocalDate today = LocalDate.now();
        LocalDate firstCompletedDay = today.minusDays(2);
        when(sentimentMapper.findAllByTable("politics_average")).thenReturn(List.of(
                sentiment(firstCompletedDay, 0.0),
                sentiment(firstCompletedDay.plusDays(1), 1.0),
                sentiment(today, 100.0)));
        when(sentimentMapper.findAllByTable("tech_average")).thenReturn(List.of());
        when(stockIndexMapper.findAll()).thenReturn(List.of(
                stock(firstCompletedDay, 0.0),
                stock(firstCompletedDay.plusDays(1), 1.0),
                stock(today, -100.0)));
        when(correlationMapper.updateCorrelation(any())).thenReturn(1);

        marketTrendService.rollingUpdateCorrelation();

        ArgumentCaptor<MarketCorrelation> captor = ArgumentCaptor.forClass(MarketCorrelation.class);
        verify(correlationMapper, times(4)).updateCorrelation(captor.capture());
        MarketCorrelation record = captor.getAllValues().stream()
                .filter(candidate -> "^GSPC".equals(candidate.getSymbol()))
                .findFirst()
                .orElseThrow();
        assertEquals(1.0, record.getCorr(), 0.000001);
        assertEquals(2, record.getSampleSize());
    }

    @Test
    void rollingUpdateRecoversWhenAnotherInstanceInsertsFirst() {
        LocalDate oldDate = LocalDate.now().minusMonths(1);
        when(sentimentMapper.findAllByTable("politics_average")).thenReturn(List.of(
                sentiment(oldDate, 0.0),
                sentiment(oldDate.plusDays(1), 1.0)));
        when(sentimentMapper.findAllByTable("tech_average")).thenReturn(List.of());
        when(stockIndexMapper.findAll()).thenReturn(List.of(
                stock(oldDate, 0.0),
                stock(oldDate.plusDays(1), 1.0)));
        when(correlationMapper.updateCorrelation(any())).thenReturn(0, 1, 0, 1, 0, 1, 0, 1);
        when(correlationMapper.insertCorrelation(any()))
                .thenThrow(new DuplicateKeyException("correlation already exists"));

        marketTrendService.rollingUpdateCorrelation();

        verify(correlationMapper, times(8)).updateCorrelation(any());
        verify(correlationMapper, times(4)).insertCorrelation(any());
    }

    @Test
    void initializationSkipsFullHistoryWhenCorrelationsAlreadyExist() {
        when(correlationMapper.findAll()).thenReturn(List.of(persisted("^GSPC", "S&P 500", 0.25, 12)));

        marketTrendService.initializeCorrelations();

        verify(sentimentMapper, never()).findAllByTable(any());
        verify(stockIndexMapper, never()).findAll();
        verify(correlationMapper, never()).updateCorrelation(any());
    }

    private SentimentAverage sentiment(LocalDate date, double score) {
        SentimentAverage average = new SentimentAverage();
        average.setDate(date);
        average.setSentimentScore(score);
        return average;
    }

    private MarketCorrelation persisted(String symbol, String name, double corr, int sampleSize) {
        MarketCorrelation record = new MarketCorrelation();
        record.setSymbol(symbol);
        record.setName(name);
        record.setCorr(corr);
        record.setSampleSize(sampleSize);
        return record;
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
