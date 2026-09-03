package ReleaseBack.Back;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.lang.NonNull;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ReleaseBack.Back.config.UserSettingsSchemaMigration;
import ReleaseBack.Back.config.UsStockIndexSchemaMigration;
import ReleaseBack.Back.config.SentimentSchemaMigration;

@Testcontainers(disabledWithoutDocker = true)
class Mysql8SchemaIntegrationTest {
    private static final List<String> SCHEMA_SCRIPTS = List.of(
            "01-user.sql",
            "02-profiles.sql",
            "03-user-settings.sql",
            "04-sentiment-average.sql",
            "05-us-stock-indices.sql",
            "07-market-correlation.sql");

    @Container
    @SuppressWarnings("resource") // The JUnit Testcontainers extension stops this container.
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("schema_test")
            .withUsername("schema_test")
            .withPassword("schema_test");

    @Test
    void productionSchemaInitializesAnEmptyMysql8DatabaseIdempotently() throws SQLException {
        DataSource dataSource = testDataSource();

        executeProductionSchema(dataSource);
        executeProductionSchema(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertEquals(0, rowCount(jdbcTemplate, "user"));
        assertEquals(0, rowCount(jdbcTemplate, "profiles"));
        assertEquals(0, rowCount(jdbcTemplate, "user_settings"));
        assertEquals(0, rowCount(jdbcTemplate, "politics_average"));
        assertEquals(0, rowCount(jdbcTemplate, "tech_average"));
        assertEquals(0, rowCount(jdbcTemplate, "us_stock_indices"));
        assertEquals(0, rowCount(jdbcTemplate, "market_correlation"));
        assertEquals(2, currencyColumnCount(jdbcTemplate));
        assertEquals(4, displayColumnCount(jdbcTemplate));
        assertEquals(6, sentimentRollingColumnCount(jdbcTemplate));
        assertEquals(8, dailyMarketColumnCount(jdbcTemplate));

        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getDatabaseProductVersion().startsWith("8."));
        }
    }

    @Test
    void migrationAddsMissingCurrencyColumnsToAnExistingMysql8Database() throws Exception {
        DataSource dataSource = testDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS user_settings");
        jdbcTemplate.execute(
                "CREATE TABLE user_settings (user_id INT PRIMARY KEY, default_page TINYINT NOT NULL DEFAULT 0)");

        UserSettingsSchemaMigration migration = new UserSettingsSchemaMigration(dataSource);
        migration.migrate();
        migration.migrate();

        assertEquals(2, currencyColumnCount(jdbcTemplate));
        assertEquals(4, displayColumnCount(jdbcTemplate));
    }

    @Test
    void migrationMakesLegacySentimentTablesAppendOnlyInMysql8() throws Exception {
        DataSource dataSource = testDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS politics_average");
        jdbcTemplate.execute("DROP TABLE IF EXISTS tech_average");
        jdbcTemplate.execute(
                "CREATE TABLE politics_average ("
                        + "ID INT PRIMARY KEY, date DATE UNIQUE, sentimentScore DOUBLE)");
        jdbcTemplate.execute(
                "CREATE TABLE tech_average ("
                        + "ID INT PRIMARY KEY, date DATE UNIQUE, sentimentScore DOUBLE)");
        jdbcTemplate.update(
                "INSERT INTO politics_average VALUES (1, '2026-08-11', 0.25)");

        try {
            SentimentSchemaMigration migration = new SentimentSchemaMigration(dataSource);
            migration.migrate();
            migration.migrate();

            assertEquals(6, sentimentRollingColumnCount(jdbcTemplate));
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT sampleCount FROM politics_average WHERE ID = 1",
                    Integer.class));
            assertEquals(0.25, jdbcTemplate.queryForObject(
                    "SELECT rollingAverage FROM politics_average WHERE ID = 1",
                    Double.class));
            assertEquals(0.0, jdbcTemplate.queryForObject(
                    "SELECT rollingStandardDeviation FROM politics_average WHERE ID = 1",
                    Double.class));
            jdbcTemplate.update(
                    "INSERT INTO politics_average "
                            + "(ID, date, sentimentScore, rollingAverage, "
                            + "rollingStandardDeviation, sampleCount) "
                            + "VALUES (2, '2026-08-11', 0.5, 0.375, 0.125, 2)");
        } finally {
            jdbcTemplate.execute("DROP TABLE IF EXISTS politics_average");
            jdbcTemplate.execute("DROP TABLE IF EXISTS tech_average");
            executeProductionSchema(dataSource);
        }
    }

    @Test
    void migrationCollapsesLegacyStockRowsInMysql8() throws Exception {
        DataSource dataSource = testDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS us_stock_indices");
        jdbcTemplate.execute(
                """
                CREATE TABLE us_stock_indices (
                    ID INT AUTO_INCREMENT PRIMARY KEY,
                    symbol VARCHAR(32) NOT NULL,
                    name VARCHAR(128) NOT NULL,
                    price DOUBLE NOT NULL,
                    change_value DOUBLE NOT NULL,
                    change_percent DOUBLE NOT NULL,
                    date DATE NOT NULL,
                    source VARCHAR(128),
                    UNIQUE (symbol, date)
                )
                """);
        jdbcTemplate.update(
                """
                INSERT INTO us_stock_indices
                    (symbol, name, price, change_value, change_percent, date, source)
                VALUES
                    ('^GSPC', 'S&P 500', 100, 1, 1.0, '2026-08-07', 'test'),
                    ('^DJI', 'Dow Jones', 100, 1, 2.0, '2026-08-07', 'test'),
                    ('^IXIC', 'NASDAQ', 100, 1, 3.0, '2026-08-07', 'test'),
                    ('^RUT', 'Russell 2000', 100, 1, 4.0, '2026-08-07', 'test'),
                    ('^GSPC', 'S&P 500', 900, 1, 9.0, '2026-08-08', 'test'),
                    ('^DJI', 'Dow Jones', 900, 1, 9.0, '2026-08-08', 'test'),
                    ('^IXIC', 'NASDAQ', 900, 1, 9.0, '2026-08-08', 'test'),
                    ('^RUT', 'Russell 2000', 900, 1, 9.0, '2026-08-08', 'test')
                """);

        try {
            UsStockIndexSchemaMigration migration = new UsStockIndexSchemaMigration(dataSource);
            migration.migrate();
            migration.migrate();

            assertEquals(1, rowCount(jdbcTemplate, "us_stock_indices"));
            assertEquals(8, dailyMarketColumnCount(jdbcTemplate));
            assertEquals(100.0, jdbcTemplate.queryForObject(
                    "SELECT sp500_price FROM us_stock_indices WHERE date = '2026-08-07'",
                    Double.class));
            assertEquals(1.0, jdbcTemplate.queryForObject(
                    "SELECT sp500_change_percent FROM us_stock_indices WHERE date = '2026-08-07'",
                    Double.class));
        } finally {
            jdbcTemplate.execute("DROP TABLE IF EXISTS us_stock_indices");
        }
    }

    @NonNull
    private DataSource testDataSource() {
        return new DriverManagerDataSource(
                requiredString(MYSQL.getJdbcUrl(), "MySQL JDBC URL"),
                requiredString(MYSQL.getUsername(), "MySQL username"),
                requiredString(MYSQL.getPassword(), "MySQL password"));
    }

    private void executeProductionSchema(@NonNull DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        for (String schemaScript : SCHEMA_SCRIPTS) {
            populator.addScript(new FileSystemResource("../Schema/" + schemaScript));
        }
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private int currencyColumnCount(JdbcTemplate jdbcTemplate) {
        return requiredInteger(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                    AND table_name = 'user_settings'
                    AND column_name IN ('default_base_currency', 'default_quote_currency')
                """,
                Integer.class));
    }

    private int displayColumnCount(JdbcTemplate jdbcTemplate) {
        return requiredInteger(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                    AND table_name = 'user_settings'
                    AND column_name IN (
                        'background_color',
                        'globe_glow_color',
                        'globe_point_color',
                        'globe_marker_color'
                    )
                """,
                Integer.class));
    }

    private int sentimentRollingColumnCount(JdbcTemplate jdbcTemplate) {
        return requiredInteger(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                    AND table_name IN ('politics_average', 'tech_average')
                    AND column_name IN (
                        'sampleCount',
                        'rollingAverage',
                        'rollingStandardDeviation'
                    )
                """,
                Integer.class));
    }

    private int dailyMarketColumnCount(JdbcTemplate jdbcTemplate) {
        return requiredInteger(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                    AND table_name = 'us_stock_indices'
                    AND column_name IN (
                        'sp500_change_percent',
                        'sp500_price',
                        'dow_jones_change_percent',
                        'dow_jones_price',
                        'nasdaq_change_percent',
                        'nasdaq_price',
                        'russell_2000_change_percent',
                        'russell_2000_price'
                    )
                """,
                Integer.class));
    }

    private int rowCount(JdbcTemplate jdbcTemplate, String tableName) {
        return requiredInteger(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Integer.class));
    }

    private int requiredInteger(Integer value) {
        return Objects.requireNonNull(value, "Expected a row count");
    }

    @NonNull
    private String requiredString(String value, String description) {
        return Objects.requireNonNull(value, description);
    }
}
