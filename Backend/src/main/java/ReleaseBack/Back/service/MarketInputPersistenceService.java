package ReleaseBack.Back.service;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ReleaseBack.Back.config.AppConfigProvider;
import ReleaseBack.Back.entity.MarketInputSource;
import ReleaseBack.Back.entity.MarketObservation;
import ReleaseBack.Back.mapper.MarketInputMapper;
import ReleaseBack.Back.mapper.MarketSignalMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MarketInputPersistenceService {
    private final Path snapshot;
    private final MarketInputMapper mapper;
    private final MarketSignalMapper state;
    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    public MarketInputPersistenceService(AppConfigProvider config, MarketInputMapper mapper, MarketSignalMapper state) {
        this(config.getSharedDirectory(), mapper, state);
    }

    MarketInputPersistenceService(Path directory, MarketInputMapper mapper, MarketSignalMapper state) {
        this.snapshot = directory.resolve("market_inputs.json");
        this.mapper = mapper;
        this.state = state;
    }

    @Transactional
    public void ingest() {
        if (!Files.exists(snapshot)) return;
        final JsonNode root;
        final String hash;
        String previousHash = state.lockInputHash();
        // Hash with a small streaming buffer; unchanged files never become a JSON object tree.
        try (FileInputStream file = new FileInputStream(snapshot.toFile())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream stream = new DigestInputStream(file, digest);
            stream.transferTo(OutputStream.nullOutputStream());
            hash = HexFormat.of().formatHex(digest.digest());
            if (hash.equals(previousHash)) return;
            // Reuse the open inode so an atomic crawler replacement cannot change the parsed file.
            file.getChannel().position(0);
            root = json.readTree(file);
        } catch (IOException exception) {
            log.warn("Cannot read market_inputs.json; retaining persisted observations", exception);
            return;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
        if (root == null || !root.path("observations").isArray() || !root.path("sources").isArray()) {
            log.warn("Invalid market input envelope; retaining persisted observations");
            return;
        }
        Instant now = Instant.now();
        for (JsonNode node : root.path("observations")) {
            MarketObservation observation;
            try {
                observation = parseObservation(node, now);
            } catch (IllegalArgumentException exception) {
                log.warn("Skipping invalid market observation: {}", exception.getMessage());
                continue;
            }
            MarketObservation old = mapper.findObservation(observation.getIndicator(), observation.getDate());
            if (old == null) {
                mapper.insertObservation(observation);
            } else if (observation.getFetchedAt() > old.getFetchedAt()) {
                // Keep first availability for unchanged values; revised values become available now.
                if (observation.getValue().equals(old.getValue())) observation.setAvailableAt(old.getAvailableAt());
                mapper.updateObservation(observation);
            }
        }
        Map<String, MarketInputSource> oldSources = new HashMap<>();
        mapper.findSources().forEach(source -> oldSources.put(source.getSource(), source));
        for (JsonNode node : root.path("sources")) {
            MarketInputSource source;
            try {
                source = parseSource(node, now);
            } catch (IllegalArgumentException exception) {
                log.warn("Skipping invalid market source status: {}", exception.getMessage());
                continue;
            }
            MarketInputSource old = oldSources.get(source.getSource());
            if (old == null) {
                mapper.insertSource(source);
                oldSources.put(source.getSource(), source);
            } else if (source.getAttemptedAt() > old.getAttemptedAt()) {
                mapper.updateSource(source);
            }
        }
        // Commit the checkpoint with the observations; failed transactions remain retryable.
        state.updateInputHash(hash);
    }

    private MarketObservation parseObservation(JsonNode node, Instant now) {
        try {
            String code = node.path("indicator").asText(), source = node.path("source").asText();
            if (!MarketInputCatalog.accepts(code, source) || !node.path("value").isNumber()) {
                throw new IllegalArgumentException("Unknown indicator/source or nonnumeric value");
            }
            double value = node.path("value").asDouble();
            LocalDate date = LocalDate.parse(node.path("date").asText());
            long fetchedAt = Instant.parse(node.path("fetchedAt").asText()).toEpochMilli();
            if (!Double.isFinite(value) || fetchedAt > now.toEpochMilli() || fetchedAt < 0
                    || date.isAfter(Instant.ofEpochMilli(fetchedAt).atZone(ZoneOffset.UTC).toLocalDate())) {
                throw new IllegalArgumentException("Nonfinite value or future timestamp");
            }
            if ((code.startsWith("cot_") || List.of("vix", "vix3m", "sp500", "put_call", "hy_spread").contains(code)) && value < 0) {
                throw new IllegalArgumentException("Negative market level");
            }
            if (code.startsWith("aaii_") && (value < 0 || value > 100)) {
                throw new IllegalArgumentException("Survey percentage outside 0..100");
            }
            MarketObservation observation = new MarketObservation();
            observation.setIndicator(code);
            observation.setDate(date);
            observation.setValue(value);
            observation.setSource(source);
            observation.setFetchedAt(fetchedAt);
            // Conservative point-in-time boundary: never backdate a newly imported history row.
            observation.setAvailableAt(now.toEpochMilli());
            return observation;
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException("Invalid observation timestamp", exception);
        }
    }

    private MarketInputSource parseSource(JsonNode node, Instant now) {
        try {
            String name = node.path("source").asText(), status = node.path("status").asText();
            long attemptedAt = Instant.parse(node.path("attemptedAt").asText()).toEpochMilli();
            if (!List.of("fred", "cboe", "cftc", "aaii", "naaim").contains(name)
                    || !List.of("ok", "error", "delayed", "partial").contains(status) || attemptedAt > now.toEpochMilli() || attemptedAt < 0) {
                throw new IllegalArgumentException("Invalid source status");
            }
            MarketInputSource source = new MarketInputSource();
            source.setSource(name);
            source.setStatus(status);
            source.setMessage(node.path("message").asText().substring(0, Math.min(255, node.path("message").asText().length())));
            source.setAttemptedAt(attemptedAt);
            return source;
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException("Invalid source timestamp", exception);
        }
    }
}
