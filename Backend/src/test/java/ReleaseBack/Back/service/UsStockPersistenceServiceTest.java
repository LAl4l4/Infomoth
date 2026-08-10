package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ReleaseBack.Back.entity.UsStockIndexRecord;
import ReleaseBack.Back.mapper.UsStockIndexMapper;

@ExtendWith(MockitoExtension.class)
class UsStockPersistenceServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private UsStockIndexMapper stockIndexMapper;

    private UsStockPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new UsStockPersistenceService(tempDir, stockIndexMapper);
    }

    @Test
    void persistsOneRowForACompleteTradingDay() throws IOException {
        String tradingDate = "2026-08-07";
        Files.writeString(tempDir.resolve("us_stock_indices.json"), """
                [
                  {"symbol":"^GSPC","price":5000.0,"changePercent":0.2,"date":"%s"},
                  {"symbol":"^DJI","price":40000.0,"changePercent":0.05,"date":"%s"},
                  {"symbol":"^IXIC","price":16000.0,"changePercent":0.3,"date":"%s"},
                  {"symbol":"^RUT","price":2000.0,"changePercent":-0.1,"date":"%s"}
                ]
                """.formatted(tradingDate, tradingDate, tradingDate, tradingDate));
        when(stockIndexMapper.updateDailyChange(any(UsStockIndexRecord.class))).thenReturn(0);

        service.persistTradingDayChanges();

        ArgumentCaptor<UsStockIndexRecord> captor = ArgumentCaptor.forClass(UsStockIndexRecord.class);
        verify(stockIndexMapper).updateDailyChange(any(UsStockIndexRecord.class));
        verify(stockIndexMapper).insertDailyChange(captor.capture());
        UsStockIndexRecord record = captor.getValue();
        assertEquals(LocalDate.parse(tradingDate), record.getDate());
        assertEquals(5000.0, record.getSp500Price());
        assertEquals(0.2, record.getSp500ChangePercent());
        assertEquals(40000.0, record.getDowJonesPrice());
        assertEquals(0.05, record.getDowJonesChangePercent());
        assertEquals(16000.0, record.getNasdaqPrice());
        assertEquals(0.3, record.getNasdaqChangePercent());
        assertEquals(2000.0, record.getRussell2000Price());
        assertEquals(-0.1, record.getRussell2000ChangePercent());
    }

    @Test
    void skipsIncompleteTradingDaysAndInvalidSnapshots() throws IOException {
        Files.writeString(tempDir.resolve("us_stock_indices.json"), """
                [
                  {"symbol":"^GSPC","price":5000.0,"changePercent":0.2,"date":"2026-08-07"},
                  {"symbol":"^DJI","price":40000.0,"changePercent":0.05,"date":"2026-08-07"},
                  {"symbol":"^IXIC","price":16000.0,"changePercent":0.3,"date":"2026-08-07"},
                  {"symbol":"^GSPC","price":5000.0,"changePercent":0.2,"date":"2026-08-08"},
                  {"symbol":"^DJI","price":40000.0,"changePercent":0.05,"date":"2026-08-08"},
                  {"symbol":"^IXIC","price":16000.0,"changePercent":0.3,"date":"2026-08-08"},
                  {"symbol":"^RUT","price":2000.0,"changePercent":-0.1,"date":"2026-08-08"},
                  {"symbol":"","price":1.0,"changePercent":1.0,"date":"not-a-date"}
                ]
                """);

        service.persistTradingDayChanges();

        verify(stockIndexMapper, never()).updateDailyChange(any(UsStockIndexRecord.class));
        verify(stockIndexMapper, never()).insertDailyChange(any(UsStockIndexRecord.class));
    }

    @Test
    void updatesAnExistingDailyRowWithoutInsertingAnotherId() throws IOException {
        Files.writeString(tempDir.resolve("us_stock_indices.json"), """
                [
                  {"symbol":"^GSPC","price":5000.0,"changePercent":0.2,"date":"2026-08-07"},
                  {"symbol":"^DJI","price":40000.0,"changePercent":0.05,"date":"2026-08-07"},
                  {"symbol":"^IXIC","price":16000.0,"changePercent":0.3,"date":"2026-08-07"},
                  {"symbol":"^RUT","price":2000.0,"changePercent":-0.1,"date":"2026-08-07"}
                ]
                """);
        when(stockIndexMapper.updateDailyChange(any(UsStockIndexRecord.class))).thenReturn(1);

        service.persistTradingDayChanges();

        verify(stockIndexMapper).updateDailyChange(any(UsStockIndexRecord.class));
        verify(stockIndexMapper, never()).insertDailyChange(any(UsStockIndexRecord.class));
    }
}
