package ReleaseBack.Back.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        this(appConfigProvider.getSharedDirectory(), stockIndexMapper);
    }

    UsStockPersistenceService(Path sharedDir, UsStockIndexMapper stockIndexMapper) {
        this.objectMapper = new ObjectMapper();
        this.stockIndexMapper = stockIndexMapper;
        this.sharedDir = sharedDir;
    }

    public void persistTradingDayChanges() {
        try {
            List<usStockIndexDTO> snapshots = objectMapper.readValue(
                    sharedDir.resolve(STOCK_FILE).toFile(),
                    new TypeReference<List<usStockIndexDTO>>() {});

            Map<LocalDate, UsStockIndexRecord> changesByDate = new LinkedHashMap<>();
            for (usStockIndexDTO snapshot : snapshots) {
                addSnapshot(changesByDate, snapshot);
            }
            for (UsStockIndexRecord record : changesByDate.values()) {
                if (!isComplete(record)) {
                    log.warn("Skipping incomplete US stock changes for trading date {}", record.getDate());
                    continue;
                }
                if (stockIndexMapper.updateDailyChange(record) == 0) {
                    stockIndexMapper.insertDailyChange(record);
                }
                log.info("Persisted US stock changes for trading date {}", record.getDate());
            }
        } catch (IOException e) {
            log.warn("Cannot read {}; it will be retried on the next run", STOCK_FILE, e);
        }
    }

    private void addSnapshot(
            Map<LocalDate, UsStockIndexRecord> changesByDate,
            usStockIndexDTO snapshot) {
        if (snapshot == null
                || snapshot.getSymbol() == null
                || snapshot.getSymbol().isBlank()
                || snapshot.getPrice() == null
                || snapshot.getChangePercent() == null
                || !Double.isFinite(snapshot.getPrice())
                || !Double.isFinite(snapshot.getChangePercent())
                || snapshot.getDate() == null) {
            log.warn("Skipping invalid US stock snapshot");
            return;
        }

        final LocalDate date;
        try {
            date = LocalDate.parse(snapshot.getDate());
        } catch (DateTimeParseException e) {
            log.warn("Skipping US stock snapshot with invalid date: {}", snapshot.getDate());
            return;
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            log.warn("Skipping US stock snapshot with non-trading weekend date: {}", date);
            return;
        }

        UsStockIndexRecord record = changesByDate.computeIfAbsent(date, ignored -> {
            UsStockIndexRecord value = new UsStockIndexRecord();
            value.setDate(date);
            return value;
        });
        switch (snapshot.getSymbol().trim()) {
            case "^GSPC" -> {
                record.setSp500Price(snapshot.getPrice());
                record.setSp500ChangePercent(snapshot.getChangePercent());
            }
            case "^DJI" -> {
                record.setDowJonesPrice(snapshot.getPrice());
                record.setDowJonesChangePercent(snapshot.getChangePercent());
            }
            case "^IXIC" -> {
                record.setNasdaqPrice(snapshot.getPrice());
                record.setNasdaqChangePercent(snapshot.getChangePercent());
            }
            case "^RUT" -> {
                record.setRussell2000Price(snapshot.getPrice());
                record.setRussell2000ChangePercent(snapshot.getChangePercent());
            }
            default -> log.warn("Skipping unsupported US stock symbol: {}", snapshot.getSymbol());
        }
    }

    private boolean isComplete(UsStockIndexRecord record) {
        return record.getSp500Price() != null
                && record.getSp500ChangePercent() != null
                && record.getDowJonesPrice() != null
                && record.getDowJonesChangePercent() != null
                && record.getNasdaqPrice() != null
                && record.getNasdaqChangePercent() != null
                && record.getRussell2000Price() != null
                && record.getRussell2000ChangePercent() != null;
    }
}
