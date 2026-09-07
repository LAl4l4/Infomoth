package ReleaseBack.Back.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "market.inputs", name = "enabled", havingValue = "true")
public class MarketInputScheduler {
    private final MarketInputPersistenceService persistence;
    private final MarketSignalSnapshotService snapshots;

    @Scheduled(initialDelayString = "${market.inputs.initial-delay-ms}", fixedDelayString = "${market.inputs.fixed-delay-ms}")
    public void ingest() {
        try {
            persistence.ingest();
        } finally {
            // News updates and calendar expiry must refresh even if market ingestion fails.
            snapshots.refresh();
        }
    }
}
