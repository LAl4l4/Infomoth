package ReleaseBack.Back.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

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
                    sentimentScore DOUBLE NOT NULL
                )
                """
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS tech_average (
                    ID INT AUTO_INCREMENT PRIMARY KEY,
                    date DATE NOT NULL,
                    sentimentScore DOUBLE NOT NULL
                )
                """
        );
        jdbcTemplate.execute("DELETE FROM politics_average");
        jdbcTemplate.execute("DELETE FROM tech_average");
    }

    @Test
    void findLatestByTableShouldReturnNewestRecordByDate() {
        jdbcTemplate.update(
                "INSERT INTO politics_average(date, sentimentScore) VALUES (?, ?)",
                LocalDate.of(2026, 5, 24), 0.12
        );
        jdbcTemplate.update(
                "INSERT INTO politics_average(date, sentimentScore) VALUES (?, ?)",
                LocalDate.of(2026, 5, 25), 0.66
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
}
