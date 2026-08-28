package ReleaseBack.Back.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import ReleaseBack.Back.entity.MarketCorrelation;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:market_correlation_mapper_db;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "mybatis.mapper-locations=classpath*:mapper/*.xml"
})
class MarketCorrelationMapperTest {

    @Autowired
    private MarketCorrelationMapper correlationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS market_correlation (
                    symbol VARCHAR(16) PRIMARY KEY,
                    name VARCHAR(64) NOT NULL,
                    corr DOUBLE,
                    sample_size INT NOT NULL
                )
                """);
        jdbcTemplate.execute("DELETE FROM market_correlation");
    }

    @Test
    void shouldInsertUpdateAndReadCorrelation() {
        MarketCorrelation correlation = correlation("^GSPC", "S&P 500", 0.25, 12);

        assertEquals(0, correlationMapper.updateCorrelation(correlation));
        assertEquals(1, correlationMapper.insertCorrelation(correlation));

        correlation.setCorr(-0.5);
        correlation.setSampleSize(13);
        assertEquals(1, correlationMapper.updateCorrelation(correlation));

        List<MarketCorrelation> results = correlationMapper.findAll();
        assertEquals(1, results.size());
        assertEquals("^GSPC", results.get(0).getSymbol());
        assertEquals(-0.5, results.get(0).getCorr());
        assertEquals(13, results.get(0).getSampleSize());
    }

    private MarketCorrelation correlation(String symbol, String name, double corr, int sampleSize) {
        MarketCorrelation value = new MarketCorrelation();
        value.setSymbol(symbol);
        value.setName(name);
        value.setCorr(corr);
        value.setSampleSize(sampleSize);
        return value;
    }
}
