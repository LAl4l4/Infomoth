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

import ReleaseBack.Back.DTO.exchangeRateDTO;
import ReleaseBack.Back.VO.TokenVO;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
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

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("SET NON_KEYWORDS USER;");
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
    }

    @Test
    void contextLoads() {
    }

    @Test
    void smokeAuthRegister() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register?username={username}&pass={pass}&email={email}",
                null,
                String.class,
                "smoke-user",
                "smoke-pass",
                "smoke@example.com"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("注册成功", response.getBody());
    }

    @Test
    void smokeDataExchangeRate() throws IOException {
        exchangeRateDTO sampleRate = loadSampleRate();
        String token = registerAndLogin("rate-user", "rate-pass");

        if (token == null) {
            System.out.println("Failed to obtain JWT token for test user");
            throw new AssertionError("Failed to obtain JWT token for test user");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
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

    private String registerAndLogin(String username, String password) {
        restTemplate.postForEntity(
                "/auth/register?username={username}&pass={pass}&email={email}",
                null,
                String.class,
                username,
                password,
                username + "@example.com"
        );

        ResponseEntity<TokenVO> loginResponse = restTemplate.postForEntity(
                "/auth/login?username={username}&pass={pass}",
                null,
                TokenVO.class,
                username,
                password
        );

        TokenVO tokenVO = loginResponse.getBody();
        if (tokenVO == null) {
            System.out.println("Login response body is null");
            throw new AssertionError("Login response body is null");
        }

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertEquals("登录成功", tokenVO.getResult());
        assertNotNull(tokenVO.getToken());
        return tokenVO.getToken();
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
