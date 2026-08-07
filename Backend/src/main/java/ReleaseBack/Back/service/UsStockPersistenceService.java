package ReleaseBack.Back.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ReleaseBack.Back.DTO.usStockIndexDTO;
import ReleaseBack.Back.config.AppConfigProvider;
import ReleaseBack.Back.entity.UsStockIndexRecord;
import ReleaseBack.Back.mapper.UsStockIndexMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UsStockPersistenceService {
    private static final String STOCK_FILE = "us_stock_indices.json";

    private final ObjectMapper objectMapper;
    private final UsStockIndexMapper stockIndexMapper;
    private final Path sharedDir;

    @Autowired
    public UsStockPersistenceService(
            AppConfigProvider appConfigProvider,
            UsStockIndexMapper stockIndexMapper) {
        this(Path.of(appConfigProvider.getSharedDirectory()), stockIndexMapper);
    }

    UsStockPersistenceService(Path sharedDir, UsStockIndexMapper stockIndexMapper) {
        this.objectMapper = new ObjectMapper();
        this.stockIndexMapper = stockIndexMapper;
        this.sharedDir = sharedDir;
    }

    public void persistTodaySnapshots() {
        LocalDate today = LocalDate.now();
        try {
            List<usStockIndexDTO> snapshots = objectMapper.readValue(
                    sharedDir.resolve(STOCK_FILE).toFile(),
                    new TypeReference<List<usStockIndexDTO>>() {});

            for (usStockIndexDTO snapshot : snapshots) {
                UsStockIndexRecord record = toRecord(snapshot, today);
                if (record == null) {
                    continue;
                }

                if (stockIndexMapper.updateSnapshot(record) == 0) {
                    stockIndexMapper.insertSnapshot(record);
                }
            }
            log.info("Persisted US stock snapshots for {}", today);
        } catch (IOException e) {
            log.warn("Cannot read {}; it will be retried on the next run", STOCK_FILE, e);
        }
    }

    private UsStockIndexRecord toRecord(usStockIndexDTO snapshot, LocalDate today) {
        if (snapshot == null
                || snapshot.getSymbol() == null
                || snapshot.getSymbol().isBlank()
                || snapshot.getName() == null
                || snapshot.getName().isBlank()
                || snapshot.getPrice() == null
                || snapshot.getChange() == null
                || snapshot.getChangePercent() == null
                || !Double.isFinite(snapshot.getPrice())
                || !Double.isFinite(snapshot.getChange())
                || !Double.isFinite(snapshot.getChangePercent())
                || snapshot.getDate() == null) {
            log.warn("Skipping invalid US stock snapshot");
            return null;
        }

        final LocalDate date;
        try {
            date = LocalDate.parse(snapshot.getDate());
        } catch (DateTimeParseException e) {
            log.warn("Skipping US stock snapshot with invalid date: {}", snapshot.getDate());
            return null;
        }

        if (!today.equals(date)) {
            log.warn("Skipping stale US stock snapshot dated {}", date);
            return null;
        }

        UsStockIndexRecord record = new UsStockIndexRecord();
        record.setSymbol(snapshot.getSymbol().trim());
        record.setName(snapshot.getName().trim());
        record.setPrice(snapshot.getPrice());
        record.setChangeValue(snapshot.getChange());
        record.setChangePercent(snapshot.getChangePercent());
        record.setDate(date);
        record.setSource(snapshot.getSource());
        return record;
    }
}
