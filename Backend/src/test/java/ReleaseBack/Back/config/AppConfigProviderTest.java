package ReleaseBack.Back.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AppConfigProviderTest {

    @Test
    void resolvesRelativeSharedDirectoryFromConfigDirectory() {
        assertEquals(
                Path.of("..", "Shared").toAbsolutePath().normalize(),
                new AppConfigProvider().getSharedDirectory());
    }
}
