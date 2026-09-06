package ReleaseBack.Back.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.OptionalDouble;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SentimentFileReader {
    private final ObjectMapper objectMapper;

    public SentimentFileReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OptionalDouble readAverage(Path file) throws IOException {
        return readSnapshot(file).average();
    }

    public Snapshot readSnapshot(Path file) throws IOException {
        byte[] contents = Files.readAllBytes(file);
        JsonNode items = objectMapper.readTree(contents);
        if (items == null || !items.isArray()) {
            throw new IOException(file.getFileName() + " must contain a JSON array");
        }

        double total = 0;
        int count = 0;
        for (JsonNode item : items) {
            JsonNode score = item.path("financeInfluence").path("sentiment_score");
            if (score.isNumber() && Double.isFinite(score.asDouble())) {
                total += score.asDouble();
                count++;
            }
        }
        OptionalDouble average = count == 0 ? OptionalDouble.empty() : OptionalDouble.of(total / count);
        return new Snapshot(average, fingerprint(contents));
    }

    private String fingerprint(byte[] contents) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contents));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Cannot fingerprint sentiment file", e);
        }
    }

    public record Snapshot(OptionalDouble average, String fingerprint) {
    }
}
