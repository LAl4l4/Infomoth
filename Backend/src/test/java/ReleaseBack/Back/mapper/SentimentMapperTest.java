package ReleaseBack.Back.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import ReleaseBack.Back.entity.SentimentAverage;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sentiment_mapper_db;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "mybatis.mapper-locations=classpath*:mapper/*.xml"
})
class SentimentMapperTest {

    @Autowired
    private SentimentMapper sentimentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS politics_average (
                    ID INT AUTO_INCREMENT PRIMARY KEY,
                    date DATE NOT NULL,
                    sentimentScore DOUBLE NOT NULL,
                    rollingAverage DOUBLE NOT NULL,
                    rollingStandardDeviation DOUBLE NOT NULL DEFAULT 0,
                    sampleCount INT NOT NULL DEFAULT 1
                )
                """
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS tech_average (
                    ID INT AUTO_INCREMENT PRIMARY KEY,
                    date DATE NOT NULL,
                    sentimentScore DOUBLE NOT NULL,
                    rollingAverage DOUBLE NOT NULL,
                    rollingStandardDeviation DOUBLE NOT NULL DEFAULT 0,
                    sampleCount INT NOT NULL DEFAULT 1
                )
                """
        );
        jdbcTemplate.execute("DELETE FROM politics_average");
        jdbcTemplate.execute("DELETE FROM tech_average");
    }

    @Test
    void findLatestByTableShouldReturnNewestRecordByDate() {
        jdbcTemplate.update(
                "INSERT INTO politics_average(date, sentimentScore, rollingAverage) VALUES (?, ?, ?)",
                LocalDate.of(2026, 5, 24), 0.12, 0.12
        );
        jdbcTemplate.update(
                "INSERT INTO politics_average(date, sentimentScore, rollingAverage) VALUES (?, ?, ?)",
                LocalDate.of(2026, 5, 25), 0.66, 0.39
        );

        SentimentAverage latest = sentimentMapper.findLatestByTable("politics_average");

        assertNotNull(latest);
        assertEquals(LocalDate.of(2026, 5, 25), latest.getDate());
        assertEquals(0.66, latest.getSentimentScore());
    }

    @Test
    void findLatestByTableShouldReturnNullWhenNoRows() {
        SentimentAverage latest = sentimentMapper.findLatestByTable("tech_average");
        assertNull(latest);
    }

    @Test
    void insertPoliticsAverageShouldAppendRawAndCumulativeValuesAcrossDates() {
        LocalDate today = LocalDate.of(2026, 8, 4);
        LocalDate tomorrow = today.plusDays(1);
        sentimentMapper.insertPoliticsAverage(today, 0.2, 0.2, 0.0, 1);
        sentimentMapper.insertPoliticsAverage(today, 0.6, 0.4, 0.2, 2);
        sentimentMapper.insertPoliticsAverage(tomorrow, -0.5, 0.1, 0.454606, 3);

        SentimentAverage latestToday = sentimentMapper.findByDate("politics_average", today);
        SentimentAverage latest = sentimentMapper.findLatestByTable("politics_average");

        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM politics_average", Integer.class));
        assertEquals(0.6, latestToday.getSentimentScore());
        assertEquals(0.4, latestToday.getRollingAverage());
        assertEquals(0.2, latestToday.getRollingStandardDeviation());
        assertEquals(2, latestToday.getSampleCount());
        assertEquals(-0.5, latest.getSentimentScore());
        assertEquals(0.1, latest.getRollingAverage(), 0.000001);
        assertEquals(3, latest.getSampleCount());
    }

    @Test
    void findByDateShouldNotFallBackToAnEarlierDay() {
        LocalDate yesterday = LocalDate.of(2026, 8, 3);
        sentimentMapper.insertTechAverage(yesterday, 0.25, 0.25, 0.0, 1);

        assertNull(sentimentMapper.findByDate("tech_average", yesterday.plusDays(1)));
        assertEquals(0.25, sentimentMapper.findByDate("tech_average", yesterday).getSentimentScore());
    }

    @Test
    void findSinceShouldReturnRecordsInAscendingDateOrder() {
        jdbcTemplate.update(
                "INSERT INTO tech_average(date, sentimentScore, rollingAverage) VALUES (?, ?, ?)",
                LocalDate.of(2026, 5, 24), 0.12, 0.12
        );
        jdbcTemplate.update(
                "INSERT INTO tech_average(date, sentimentScore, rollingAverage) VALUES (?, ?, ?)",
                LocalDate.of(2026, 5, 26), 0.66, 0.39
        );

        List<SentimentAverage> results = sentimentMapper.findSince(
                "tech_average", LocalDate.of(2026, 5, 25));

        assertEquals(1, results.size());
        assertEquals(LocalDate.of(2026, 5, 26), results.get(0).getDate());
    }
}
