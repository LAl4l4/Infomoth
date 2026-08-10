package ReleaseBack.Back.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class UserSettingsSchemaMigrationTest {
    @Test
    void addsOnlyMissingCurrencyColumnsAndCanRunAgain() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:user_settings_migration;MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute(
                    "CREATE TABLE user_settings (user_id INT PRIMARY KEY, default_page TINYINT NOT NULL DEFAULT 0)");
        }

        UserSettingsSchemaMigration migration = new UserSettingsSchemaMigration(dataSource);
        migration.migrate();
        migration.migrate();

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(2, countCurrencyColumns(connection));
        }
    }

    private int countCurrencyColumns(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                                + "WHERE TABLE_NAME = 'USER_SETTINGS' "
                                + "AND COLUMN_NAME IN ('DEFAULT_BASE_CURRENCY', 'DEFAULT_QUOTE_CURRENCY')")) {
            result.next();
            return result.getInt(1);
        }
    }
}
