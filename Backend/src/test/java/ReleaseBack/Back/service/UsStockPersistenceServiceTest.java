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
    void persistsTodaysSnapshotsAndUpdatesExistingRows() throws IOException {
        String today = LocalDate.now().toString();
        Files.writeString(tempDir.resolve("us_stock_indices.json"), """
                [
                  {"symbol":"^GSPC","name":"S&P 500","price":5000.0,"change":10.0,"changePercent":0.2,"date":"%s"},
                  {"symbol":"^DJI","name":"Dow Jones","price":40000.0,"change":20.0,"changePercent":0.05,"date":"%s"}
                ]
                """.formatted(today, today));
        when(stockIndexMapper.updateSnapshot(any(UsStockIndexRecord.class)))
                .thenReturn(1, 0);

        service.persistTodaySnapshots();

        verify(stockIndexMapper, org.mockito.Mockito.times(2))
                .updateSnapshot(any(UsStockIndexRecord.class));
        verify(stockIndexMapper).insertSnapshot(any(UsStockIndexRecord.class));
    }

    @Test
    void skipsInvalidAndStaleSnapshots() throws IOException {
        Files.writeString(tempDir.resolve("us_stock_indices.json"), """
                [
                  {"symbol":"^GSPC","name":"S&P 500","price":5000.0,"change":10.0,"changePercent":0.2,"date":"2020-01-01"},
                  {"symbol":"","name":"bad","price":1.0,"change":1.0,"changePercent":1.0,"date":"%s"}
                ]
                """.formatted(LocalDate.now()));

        service.persistTodaySnapshots();

        verify(stockIndexMapper, never()).updateSnapshot(any(UsStockIndexRecord.class));
        verify(stockIndexMapper, never()).insertSnapshot(any(UsStockIndexRecord.class));
    }

    @Test
    void mapsTheSnapshotFields() throws IOException {
        Files.writeString(tempDir.resolve("us_stock_indices.json"), """
                [{"symbol":"^GSPC","name":"S&P 500","price":5000.0,"change":10.0,"changePercent":0.2,"date":"%s","source":"test"}]
                """.formatted(LocalDate.now()));
        when(stockIndexMapper.updateSnapshot(any(UsStockIndexRecord.class))).thenReturn(0);

        service.persistTodaySnapshots();

        ArgumentCaptor<UsStockIndexRecord> captor = ArgumentCaptor.forClass(UsStockIndexRecord.class);
        verify(stockIndexMapper).insertSnapshot(captor.capture());
        UsStockIndexRecord record = captor.getValue();
        assertEquals("^GSPC", record.getSymbol());
        assertEquals(10.0, record.getChangeValue());
        assertEquals("test", record.getSource());
    }
}
