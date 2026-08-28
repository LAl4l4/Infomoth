package ReleaseBack.Back;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema_init_db;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class SchemaInitializationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAllRequiredTablesForAnEmptyDatabase() {
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM profiles", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_settings", Integer.class));
        assertEquals(4, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'USER_SETTINGS'
                  AND COLUMN_NAME IN (
                    'BACKGROUND_COLOR',
                    'GLOBE_GLOW_COLOR',
                    'GLOBE_POINT_COLOR',
                    'GLOBE_MARKER_COLOR'
                  )
                """,
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM politics_average", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tech_average", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME IN ('POLITICS_AVERAGE', 'TECH_AVERAGE')
                  AND COLUMN_NAME = 'SAMPLECOUNT'
                """,
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM us_stock_indices", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_correlation", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'MARKET_CORRELATION'
                  AND COLUMN_NAME = 'SAMPLE_SIZE'
                """,
                Integer.class));
        assertEquals(8, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'US_STOCK_INDICES'
                  AND COLUMN_NAME IN (
                    'SP500_CHANGE_PERCENT',
                    'SP500_PRICE',
                    'DOW_JONES_CHANGE_PERCENT',
                    'DOW_JONES_PRICE',
                    'NASDAQ_CHANGE_PERCENT',
                    'NASDAQ_PRICE',
                    'RUSSELL_2000_CHANGE_PERCENT',
                    'RUSSELL_2000_PRICE'
                  )
                """,
                Integer.class));
    }
}
