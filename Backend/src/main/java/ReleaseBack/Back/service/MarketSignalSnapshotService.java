package ReleaseBack.Back.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ReleaseBack.Back.DTO.MarketSignalDTO;
import ReleaseBack.Back.entity.MarketObservation;
import ReleaseBack.Back.mapper.MarketInputMapper;
import ReleaseBack.Back.mapper.MarketSignalMapper;
import ReleaseBack.Back.mapper.SentimentMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketSignalSnapshotService {
    private final MarketInputMapper inputs;
    private final MarketSignalMapper snapshots;
    private final SentimentMapper sentiments;
    private final MarketPredictionModel model;
    private final ObjectMapper json;

    @Transactional
    public void refresh() {
        String inputHash = snapshots.lockInputHash();
        // Capture time after waiting for ingestion, so freshly committed rows are already available.
        refreshAt(Instant.now(), inputHash);
    }

    // The caller owns the transaction; a fixed instant makes expiry tests deterministic.
    void refreshAt(Instant now) {
        refreshAt(now, snapshots.lockInputHash());
    }

    private void refreshAt(Instant now, String inputHash) {
        var today = now.atZone(ZoneOffset.UTC).toLocalDate();
        var categories = new String[]{"tech", "politics"};
        var samples = Arrays.stream(categories).map(category -> sentiments.findLatestByTable(category + "_average")).toList();
        var sources = inputs.findSources();
        try {
            // Include UTC date so stale inputs expire even when every source stops updating.
            String key = json.writeValueAsString(Arrays.asList(inputHash, today, samples, sources,
                model.getClass().getName(), model.cacheVersion(), MarketInputCatalog.SPECS.toString()));
            String signature = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(key.getBytes(StandardCharsets.UTF_8)));
            if (signature.equals(snapshots.findSignature())) return;

            var observations = new ArrayList<MarketObservation>();
            for (var spec : MarketInputCatalog.SPECS) {
                if (MarketInputCatalog.accepts(spec.code(), spec.source())) {
                    // Match the existing chart window without materializing the full raw history.
                    observations.addAll(inputs.findRecent(spec.code(), today.minusDays(400), today, now.toEpochMilli()));
                }
            }
            for (int index = 0; index < categories.length; index++) {
                var sample = samples.get(index);
                Double normalized = SentimentScoreCalculator.normalizedScore(sample);
                if (normalized == null) continue;
                MarketObservation observation = new MarketObservation();
                observation.setIndicator(categories[index] + "_sentiment");
                observation.setDate(sample.getDate());
                observation.setValue(normalized);
                observation.setSource("news");
                // Reuse persisted rolling statistics; legacy news has no publication timestamp.
                observations.add(observation);
            }
            var response = new MarketSignalDTO(model.predict(observations, now), sources);
            snapshots.updateSnapshot(signature, json.writeValueAsString(response));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            // Roll back on failure, leaving the last successful response available to readers.
            throw new IllegalStateException("Cannot persist market signal snapshot", exception);
        }
    }
}
