package ReleaseBack.Back.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class SentimentSchemaMigrationTest {
    @Test
    void addsSampleCountToBothLegacyTablesAndCanRunAgain() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:sentiment_migration;MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute(
                    "CREATE TABLE politics_average (ID INT PRIMARY KEY, date DATE, sentimentScore DOUBLE)");
            connection.createStatement().execute(
                    "CREATE TABLE tech_average (ID INT PRIMARY KEY, date DATE, sentimentScore DOUBLE)");
            connection.createStatement().execute(
                    "INSERT INTO politics_average VALUES (1, DATE '2026-08-11', 0.25)");
        }

        SentimentSchemaMigration migration = new SentimentSchemaMigration(dataSource);
        migration.migrate();
        migration.migrate();

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(2, countSampleColumns(connection));
            assertEquals(1, sampleCount(connection, "politics_average"));
        }
    }

    private int countSampleColumns(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                                + "WHERE TABLE_NAME IN ('POLITICS_AVERAGE', 'TECH_AVERAGE') "
                                + "AND COLUMN_NAME = 'SAMPLECOUNT'")) {
            result.next();
            return result.getInt(1);
        }
    }

    private int sampleCount(Connection connection, String table) throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT sampleCount FROM " + table + " WHERE ID = 1")) {
            result.next();
            return result.getInt(1);
        }
    }
}
