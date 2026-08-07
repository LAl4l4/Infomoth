package ReleaseBack.Back.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sentiment.persistence", name = "enabled", havingValue = "true")
public class SentimentPersistenceScheduler {
    private final SentimentPersistenceService sentimentPersistenceService;
    private final UsStockPersistenceService usStockPersistenceService;

    @Scheduled(
            initialDelayString = "${sentiment.persistence.initial-delay-ms}",
            fixedDelayString = "${sentiment.persistence.fixed-delay-ms}")
    public void persistSentimentAverages() {
        sentimentPersistenceService.persistTodayAverages();
        usStockPersistenceService.persistTodaySnapshots();
    }
}
