package ReleaseBack.Back.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class UsStockIndexSchemaMigrationTest {
    @Test
    void collapsesLegacySymbolRowsIntoOneRowPerTradingDay() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:us_stock_migration;MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(
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
            statement.execute(
                    """
                    INSERT INTO us_stock_indices
                        (symbol, name, price, change_value, change_percent, date, source)
                    VALUES
                        ('^GSPC', 'S&P 500', 100, 1, 1.0, '2026-08-06', 'test'),
                        ('^DJI', 'Dow Jones', 100, 1, 2.0, '2026-08-06', 'test'),
                        ('^IXIC', 'NASDAQ', 100, 1, 3.0, '2026-08-06', 'test'),
                        ('^RUT', 'Russell 2000', 100, 1, 4.0, '2026-08-06', 'test'),
                        ('^GSPC', 'S&P 500', 100, 1, 1.5, '2026-08-07', 'test'),
                        ('^DJI', 'Dow Jones', 100, 1, 2.5, '2026-08-07', 'test'),
                        ('^IXIC', 'NASDAQ', 100, 1, 3.5, '2026-08-07', 'test'),
                        ('^RUT', 'Russell 2000', 100, 1, 4.5, '2026-08-07', 'test'),
                        ('^GSPC', 'S&P 500', 900, 1, 9.0, '2026-08-08', 'test'),
                        ('^DJI', 'Dow Jones', 900, 1, 9.0, '2026-08-08', 'test'),
                        ('^IXIC', 'NASDAQ', 900, 1, 9.0, '2026-08-08', 'test'),
                        ('^RUT', 'Russell 2000', 900, 1, 9.0, '2026-08-08', 'test')
                    """);
        }

        UsStockIndexSchemaMigration migration = new UsStockIndexSchemaMigration(dataSource);
        migration.migrate();
        migration.migrate();

        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery(
                    "SELECT COUNT(*), MAX(sp500_price), MAX(sp500_change_percent), "
                            + "MAX(russell_2000_price), MAX(russell_2000_change_percent) "
                            + "FROM us_stock_indices")) {
                rows.next();
                assertEquals(2, rows.getInt(1));
                assertEquals(100.0, rows.getDouble(2));
                assertEquals(1.5, rows.getDouble(3));
                assertEquals(100.0, rows.getDouble(4));
                assertEquals(4.5, rows.getDouble(5));
            }
            try (var columns = connection.getMetaData().getColumns(null, null, "US_STOCK_INDICES", "SYMBOL")) {
                assertFalse(columns.next());
            }
        }
    }
}
