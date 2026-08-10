package ReleaseBack.Back.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/** Migrates the legacy four-rows-per-day stock table to one row per trading day. */
@Component
@DependsOn("dataSourceScriptDatabaseInitializer")
public class UsStockIndexSchemaMigration implements InitializingBean {
    private static final String TABLE_NAME = "us_stock_indices";
    private static final String TEMP_TABLE_NAME = "us_stock_indices_v2";
    private static final String LEGACY_TABLE_NAME = "us_stock_indices_legacy";

    private final DataSource dataSource;

    public UsStockIndexSchemaMigration(@Qualifier("dataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        migrate();
    }

    public void migrate() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!hasColumn(connection.getMetaData(), TABLE_NAME, "symbol")) {
                dropTableIfExists(connection, TEMP_TABLE_NAME);
                dropTableIfExists(connection, LEGACY_TABLE_NAME);
                return;
            }

            dropTableIfExists(connection, TEMP_TABLE_NAME);
            dropTableIfExists(connection, LEGACY_TABLE_NAME);
            createDailyTable(connection, TEMP_TABLE_NAME);
            copyLegacyRows(connection);
            deleteWeekendRows(connection, TEMP_TABLE_NAME);

            int sourceDates = countTradingDates(connection, TABLE_NAME);
            int migratedDates = count(connection, "SELECT COUNT(*) FROM " + TEMP_TABLE_NAME);
            if (sourceDates != migratedDates) {
                throw new SQLException(
                        "Cannot migrate US stock history: not every trading day has all four indices");
            }

            replaceLegacyTable(connection);
            dropTableIfExists(connection, LEGACY_TABLE_NAME);
        }
    }

    private void createDailyTable(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE " + tableName + " ("
                            + "ID INT AUTO_INCREMENT PRIMARY KEY, "
                            + "date DATE NOT NULL UNIQUE, "
                            + "sp500_price DOUBLE NOT NULL, "
                            + "sp500_change_percent DOUBLE NOT NULL, "
                            + "dow_jones_price DOUBLE NOT NULL, "
                            + "dow_jones_change_percent DOUBLE NOT NULL, "
                            + "nasdaq_price DOUBLE NOT NULL, "
                            + "nasdaq_change_percent DOUBLE NOT NULL, "
                            + "russell_2000_price DOUBLE NOT NULL, "
                            + "russell_2000_change_percent DOUBLE NOT NULL"
                            + ")");
        }
    }

    private void copyLegacyRows(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO " + TEMP_TABLE_NAME + " ("
                            + "date, sp500_price, sp500_change_percent, "
                            + "dow_jones_price, dow_jones_change_percent, "
                            + "nasdaq_price, nasdaq_change_percent, "
                            + "russell_2000_price, russell_2000_change_percent"
                            + ") SELECT date, "
                            + "MAX(CASE WHEN symbol = '^GSPC' THEN price END), "
                            + "MAX(CASE WHEN symbol = '^GSPC' THEN change_percent END), "
                            + "MAX(CASE WHEN symbol = '^DJI' THEN price END), "
                            + "MAX(CASE WHEN symbol = '^DJI' THEN change_percent END), "
                            + "MAX(CASE WHEN symbol = '^IXIC' THEN price END), "
                            + "MAX(CASE WHEN symbol = '^IXIC' THEN change_percent END), "
                            + "MAX(CASE WHEN symbol = '^RUT' THEN price END), "
                            + "MAX(CASE WHEN symbol = '^RUT' THEN change_percent END) "
                            + "FROM " + TABLE_NAME + " GROUP BY date "
                            + "HAVING COUNT(DISTINCT CASE WHEN symbol IN "
                            + "('^GSPC', '^DJI', '^IXIC', '^RUT') THEN symbol END) = 4");
        }
    }

    private void replaceLegacyTable(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        try (Statement statement = connection.createStatement()) {
            if (productName != null && productName.toLowerCase().contains("mysql")) {
                statement.execute(
                        "RENAME TABLE " + TABLE_NAME + " TO " + LEGACY_TABLE_NAME
                                + ", " + TEMP_TABLE_NAME + " TO " + TABLE_NAME);
                return;
            }

            statement.execute("ALTER TABLE " + TABLE_NAME + " RENAME TO " + LEGACY_TABLE_NAME);
            statement.execute("ALTER TABLE " + TEMP_TABLE_NAME + " RENAME TO " + TABLE_NAME);
        }
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private int countTradingDates(Connection connection, String tableName) throws SQLException {
        int count = 0;
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT DISTINCT date FROM " + tableName)) {
            while (result.next()) {
                if (!isWeekend(result.getDate(1).toLocalDate())) {
                    count++;
                }
            }
        }
        return count;
    }

    private void deleteWeekendRows(Connection connection, String tableName) throws SQLException {
        List<LocalDate> weekendDates = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT date FROM " + tableName)) {
            while (result.next()) {
                LocalDate date = result.getDate(1).toLocalDate();
                if (isWeekend(date)) {
                    weekendDates.add(date);
                }
            }
        }

        try (var statement = connection.prepareStatement("DELETE FROM " + tableName + " WHERE date = ?")) {
            for (LocalDate date : weekendDates) {
                statement.setObject(1, date);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private void dropTableIfExists(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + tableName);
        }
    }

    private boolean hasColumn(DatabaseMetaData metadata, String tableName, String columnName)
            throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, null, null, null)) {
            while (columns.next()) {
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
