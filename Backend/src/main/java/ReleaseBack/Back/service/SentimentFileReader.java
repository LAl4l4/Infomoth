package ReleaseBack.Back.service;

import java.io.IOException;
import java.nio.file.Path;
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
        JsonNode items = objectMapper.readTree(file.toFile());
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
        return count == 0 ? OptionalDouble.empty() : OptionalDouble.of(total / count);
    }
}
