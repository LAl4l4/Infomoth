package ReleaseBack.Back.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class SentimentSchemaMigrationTest {
    @Test
    void migratesLegacyRowsToAppendOnlyRollingSamplesAndCanRunAgain() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:sentiment_migration;MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute(
                    "CREATE TABLE politics_average ("
                            + "ID INT PRIMARY KEY, date DATE UNIQUE, sentimentScore DOUBLE, "
                            + "sampleCount INT NOT NULL DEFAULT 1)");
            connection.createStatement().execute(
                    "CREATE TABLE tech_average ("
                            + "ID INT PRIMARY KEY, date DATE UNIQUE, sentimentScore DOUBLE)");
            connection.createStatement().execute(
                    "INSERT INTO politics_average VALUES (1, DATE '2026-08-11', 0.25, 4)");
            connection.createStatement().execute(
                    "INSERT INTO politics_average VALUES (2, DATE '2026-08-12', 0.75, 7)");
        }

        SentimentSchemaMigration migration = new SentimentSchemaMigration(dataSource);
        migration.migrate();
        migration.migrate();

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(6, countRollingColumns(connection));
            assertEquals(1, sampleCount(connection, "politics_average", 1));
            assertEquals(2, sampleCount(connection, "politics_average", 2));
            assertEquals(0.5, rollingAverage(connection, "politics_average", 2), 0.000001);
            assertEquals(0.353553, rollingStandardDeviation(connection, "politics_average", 2), 0.000001);
            connection.createStatement().execute(
                    "INSERT INTO politics_average "
                            + "(ID, date, sentimentScore, rollingAverage, "
                            + "rollingStandardDeviation, sampleCount) "
                            + "VALUES (3, DATE '2026-08-12', 0.5, 0.5, 0.25, 3)");
        }
    }

    private int countRollingColumns(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                                + "WHERE TABLE_NAME IN ('POLITICS_AVERAGE', 'TECH_AVERAGE') "
                                + "AND COLUMN_NAME IN ("
                                + "'SAMPLECOUNT', 'ROLLINGAVERAGE', 'ROLLINGSTANDARDDEVIATION')")) {
            result.next();
            return result.getInt(1);
        }
    }

    private int sampleCount(Connection connection, String table, int id) throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT sampleCount FROM " + table + " WHERE ID = " + id)) {
            result.next();
            return result.getInt(1);
        }
    }

    private double rollingAverage(Connection connection, String table, int id) throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT rollingAverage FROM " + table + " WHERE ID = " + id)) {
            result.next();
            return result.getDouble(1);
        }
    }

    private double rollingStandardDeviation(Connection connection, String table, int id)
            throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT rollingStandardDeviation FROM " + table + " WHERE ID = " + id)) {
            result.next();
            return result.getDouble(1);
        }
    }
}
