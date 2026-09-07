package ReleaseBack.Back;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ReleaseBack.Back.DTO.DisplaySettingsDTO;
import ReleaseBack.Back.DTO.SettingsDTO;
import ReleaseBack.Back.DTO.exchangeRateDTO;
import ReleaseBack.Back.VO.TokenVO;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "market.inputs.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:smoke_db;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "mybatis.mapper-locations=classpath*:mapper/*.xml"
})
class ApplicationSmokeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReleaseBack.Back.service.MarketSignalSnapshotService signalSnapshots;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("SET NON_KEYWORDS USER;");
        jdbcTemplate.execute("DROP TABLE IF EXISTS user_settings");
        jdbcTemplate.execute("DROP TABLE IF EXISTS profiles");
        jdbcTemplate.execute("DROP TABLE IF EXISTS user");
        jdbcTemplate.execute(
                """
                CREATE TABLE user (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) NOT NULL,
                    password VARCHAR(100) NOT NULL,
                    email VARCHAR(200) NOT NULL
                )
                """
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE profiles (
                    user_id INT PRIMARY KEY,
                    bio VARCHAR(255),
                    avatar_url VARCHAR(255),
                    birthday DATE,
                    gender VARCHAR(30)
                )
                """
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE user_settings (
                    user_id INT PRIMARY KEY,
                    default_page TINYINT NOT NULL DEFAULT 0,
                    default_base_currency VARCHAR(10) NOT NULL DEFAULT 'USD',
                    default_quote_currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
                    background_color VARCHAR(7) NOT NULL DEFAULT '#0C101C',
                    globe_glow_color VARCHAR(7) NOT NULL DEFAULT '#00FFC6',
                    globe_point_color VARCHAR(7) NOT NULL DEFAULT '#FFFFFF',
                    globe_marker_color VARCHAR(7) NOT NULL DEFAULT '#00E5FF'
                )
                """
        );
    }

    @Test
    void contextLoads() {
    }

    @Test
    void marketSignalRequiresAuthAndReturnsMockContract() {
        jdbcTemplate.execute("UPDATE market_signal_state SET signature = NULL, payload = NULL WHERE id = 1");
        assertEquals(HttpStatus.UNAUTHORIZED,
                restTemplate.getForEntity("/data/market-signal", String.class).getStatusCode());
        String cookie = registerAndLogin("signal-user", "signal-pass");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, restTemplate.exchange("/data/market-signal", HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getStatusCode());
        signalSnapshots.refresh();
        var response = restTemplate.exchange("/data/market-signal", HttpMethod.GET,
                new HttpEntity<>(headers), com.fasterxml.jackson.databind.JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("linear-mock-v1", response.getBody().path("prediction").path("modelVersion").asText());
        assertTrue(response.getBody().path("prediction").path("score").isNull());
        assertEquals(17, response.getBody().path("prediction").path("inputs").size());
    }

    @Test
    void smokeAuthRegister() {
        ResponseEntity<TokenVO> response = restTemplate.postForEntity(
                "/auth/register",
                java.util.Map.of("username", "smoke-user", "pass", "smoke-pass", "email", "smoke@example.com"),
                TokenVO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void smokeDataExchangeRate() throws IOException {
        exchangeRateDTO sampleRate = loadSampleRate();
        String authCookie = registerAndLogin("rate-user", "rate-pass");

        if (authCookie == null) {
            System.out.println("Failed to obtain auth cookie for test user");
            throw new AssertionError("Failed to obtain auth cookie for test user");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, authCookie);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Double> response = restTemplate.exchange(
                "/data/exchangerate?base={base}&quote={quote}",
                HttpMethod.GET,
                request,
                Double.class,
                sampleRate.getBase(),
                sampleRate.getQuote()
        );

        Double rate = response.getBody();
        if (rate == null) {
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body is null");
            throw new AssertionError("Response body is null");
        }

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sampleRate.getRate(), rate, 0.000001);
    }

    @Test
    void smokeSettingsPersistence() {
        String authCookie = registerAndLogin("settings-user", "settings-pass");
        HttpHeaders headers = new HttpHeaders();
        if (authCookie == null) {
            System.out.println("Failed to obtain auth cookie for test user");
            throw new AssertionError("Failed to obtain auth cookie for test user");
        }
        headers.add(HttpHeaders.COOKIE, authCookie);

        ResponseEntity<SettingsDTO> initialResponse = restTemplate.exchange(
                "/settings/general",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SettingsDTO.class);

        assertEquals(HttpStatus.OK, initialResponse.getStatusCode());
        SettingsDTO initialBody = initialResponse.getBody();
        if (initialBody == null) {
            System.out.println("Initial settings response body is null");
            throw new AssertionError("Initial settings response body is null");
        }
        assertEquals(0, initialBody.getDefaultPage());

        ResponseEntity<SettingsDTO> updateResponse = restTemplate.exchange(
                "/settings/general",
                HttpMethod.PUT,
                new HttpEntity<>(new SettingsDTO(4), headers),
                SettingsDTO.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        SettingsDTO updatedBody = updateResponse.getBody();
        if (updatedBody == null) {
            System.out.println("Update settings response body is null");
            throw new AssertionError("Update settings response body is null");
        }
        assertEquals(4, updatedBody.getDefaultPage());

        ResponseEntity<SettingsDTO> persistedResponse = restTemplate.exchange(
                "/settings/general",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SettingsDTO.class);

        assertEquals(HttpStatus.OK, persistedResponse.getStatusCode());
        SettingsDTO persistedBody = persistedResponse.getBody();
        if (persistedBody == null) {
            System.out.println("Persisted settings response body is null");
            throw new AssertionError("Persisted settings response body is null");
        }
        assertEquals(4, persistedBody.getDefaultPage());
    }

    @Test
    void smokeDisplaySettingsPersistence() {
        String authCookie = registerAndLogin("display-user", "display-pass");
        HttpHeaders headers = new HttpHeaders();
        assertNotNull(authCookie);
        headers.add(HttpHeaders.COOKIE, authCookie);

        ResponseEntity<DisplaySettingsDTO> initialResponse = restTemplate.exchange(
                "/settings/display",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                DisplaySettingsDTO.class);

        assertEquals(HttpStatus.OK, initialResponse.getStatusCode());
        assertNotNull(initialResponse.getBody());
        assertEquals("#0C101C", initialResponse.getBody().getBackgroundColor());

        DisplaySettingsDTO custom = new DisplaySettingsDTO(
                "#112233", "#445566", "#778899", "#AABBCC");
        ResponseEntity<DisplaySettingsDTO> updateResponse = restTemplate.exchange(
                "/settings/display",
                HttpMethod.PUT,
                new HttpEntity<>(custom, headers),
                DisplaySettingsDTO.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertNotNull(updateResponse.getBody());
        assertEquals("#AABBCC", updateResponse.getBody().getGlobeMarkerColor());

        ResponseEntity<DisplaySettingsDTO> persistedResponse = restTemplate.exchange(
                "/settings/display",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                DisplaySettingsDTO.class);

        assertEquals(HttpStatus.OK, persistedResponse.getStatusCode());
        assertNotNull(persistedResponse.getBody());
        assertEquals("#112233", persistedResponse.getBody().getBackgroundColor());
    }

    private String registerAndLogin(String username, String password) {
        restTemplate.postForEntity(
                "/auth/register",
                java.util.Map.of("username", username, "pass", password, "email", username + "@example.com"),
                TokenVO.class
        );

        ResponseEntity<TokenVO> loginResponse = restTemplate.postForEntity(
                "/auth/login",
                java.util.Map.of("username", username, "pass", password),
                TokenVO.class
        );

        TokenVO tokenVO = loginResponse.getBody();
        if (tokenVO == null) {
            System.out.println("Login response body is null");
            throw new AssertionError("Login response body is null");
        }

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertEquals("登录成功", tokenVO.getResult());
        String setCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private exchangeRateDTO loadSampleRate() throws IOException {
        Path file = Path.of("..", "Shared", "exchangeRates.json");
        assertTrue(Files.exists(file), "exchangeRates.json not found at " + file.toAbsolutePath());
        List<exchangeRateDTO> rates = objectMapper.readValue(
                file.toFile(),
                new TypeReference<List<exchangeRateDTO>>() {}
        );
        assertFalse(rates.isEmpty(), "exchangeRates.json is empty");
        return rates.stream()
                .filter(rate -> rate.getBase() != null && rate.getQuote() != null && rate.getRate() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid exchange rate entries found"));
    }
}
