package ReleaseBack.Back.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "stock.persistence", name = "enabled", havingValue = "true")
public class UsStockPersistenceScheduler {
    private final UsStockPersistenceService usStockPersistenceService;

    @Scheduled(
            initialDelayString = "${stock.persistence.initial-delay-ms}",
            fixedDelayString = "${stock.persistence.fixed-delay-ms}")
    public void persistLatestTradingDay() {
        usStockPersistenceService.persistTradingDayChanges();
    }
}
