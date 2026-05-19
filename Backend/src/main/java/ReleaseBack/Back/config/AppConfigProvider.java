package ReleaseBack.Back.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AppConfigProvider {
    private static final Path CONFIG_PATH = Paths.get("..", "Config", "app-config.json");

    private final JsonNode root;

    public AppConfigProvider() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            byte[] payload = Files.readAllBytes(CONFIG_PATH);
            this.root = objectMapper.readTree(payload);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load app config from " + CONFIG_PATH.toAbsolutePath(), e);
        }
    }

    public String[] getCorsAllowedOrigins() {
        JsonNode originsNode = root.path("backend").path("corsAllowedOrigins");
        if (!originsNode.isArray() || originsNode.isEmpty()) {
            throw new IllegalStateException("backend.corsAllowedOrigins is missing in app config");
        }

        List<String> origins = new ArrayList<>();
        for (JsonNode originNode : originsNode) {
            origins.add(originNode.asText());
        }
        return origins.toArray(new String[0]);
    }
}
