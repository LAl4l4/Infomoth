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
    private static final Path CONFIG_PATH = Paths.get("..", "Config", "app-config.json")
            .toAbsolutePath()
            .normalize();

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

    public String getDatabaseName() {
        String databaseName = root.path("mysql").path("database").asText("").trim();
        if (!databaseName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("mysql.database is missing or invalid in app config");
        }
        return databaseName;
    }

    public MysqlConnection getMysqlConnection() {
        JsonNode mysqlNode = root.path("mysql");
        String host = mysqlNode.path("host").asText("").trim();
        int port = mysqlNode.path("port").asInt(0);
        String user = mysqlNode.path("user").asText("").trim();
        String password = mysqlNode.path("password").asText();

        if (host.isEmpty() || port < 1 || port > 65535 || user.isEmpty()) {
            throw new IllegalStateException("mysql is missing or invalid in app config");
        }
        return new MysqlConnection(host, port, user, password);
    }

    public Path getSharedDirectory() {
        String directory = root.path("shared").path("directory").asText("").trim();
        if (directory.isEmpty()) {
            throw new IllegalStateException("shared.directory is missing in app config");
        }
        Path sharedDirectory = Path.of(directory);
        return sharedDirectory.isAbsolute()
                ? sharedDirectory
                : CONFIG_PATH.getParent().resolve(sharedDirectory).normalize();
    }

    public record MysqlConnection(String host, int port, String user, String password) {
    }
}
