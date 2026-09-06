package ReleaseBack.Back.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/** Migrates legacy sentiment rows to append-only samples with cumulative statistics. */
@Component
@DependsOn("dataSourceScriptDatabaseInitializer")
public class SentimentSchemaMigration implements InitializingBean {
    private static final String[] TABLE_NAMES = {"politics_average", "tech_average"};

    private final DataSource dataSource;

    public SentimentSchemaMigration(@Qualifier("dataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        migrate();
    }

    public void migrate() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            for (String tableName : TABLE_NAMES) {
                addColumnIfMissing(connection, tableName, "sampleCount", "INT NOT NULL DEFAULT 1");
                addColumnIfMissing(connection, tableName, "rollingAverage", "DOUBLE");
                addColumnIfMissing(connection, tableName, "rollingStandardDeviation", "DOUBLE");
                addColumnIfMissing(connection, tableName, "sampleVarianceCorrected", "BOOLEAN NOT NULL DEFAULT FALSE");
                initializeRollingValues(connection, tableName);
                dropDateUniqueness(connection, tableName);
            }
        }
    }

    private void addColumnIfMissing(
            Connection connection,
            String tableName,
            String columnName,
            String columnDefinition) throws SQLException {
        if (hasColumn(connection.getMetaData(), tableName, columnName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private void initializeRollingValues(Connection connection, String tableName) throws SQLException {
        if (!hasNullRollingStatistics(connection, tableName)
                && !hasUncorrectedSampleVariance(connection, tableName)) {
            return;
        }

        List<LegacySentimentRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT ID, sentimentScore FROM " + tableName + " ORDER BY ID")) {
            while (result.next()) {
                rows.add(new LegacySentimentRow(
                        result.getInt("ID"),
                        result.getDouble("sentimentScore")));
            }
        }

        int cumulativeCount = 0;
        double rollingAverage = 0;
        double rollingM2 = 0;
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE " + tableName + " SET rollingAverage = ?, "
                        + "rollingStandardDeviation = ?, sampleCount = ?, "
                        + "sampleVarianceCorrected = TRUE WHERE ID = ?")) {
            for (LegacySentimentRow row : rows) {
                cumulativeCount++;
                double delta = row.sentimentScore() - rollingAverage;
                rollingAverage += delta / cumulativeCount;
                rollingM2 += delta * (row.sentimentScore() - rollingAverage);
                double rollingStandardDeviation = cumulativeCount <= 1
                        ? 0.0
                        : Math.sqrt(Math.max(0.0, rollingM2 / (cumulativeCount - 1)));

                update.setDouble(1, rollingAverage);
                update.setDouble(2, rollingStandardDeviation);
                update.setInt(3, cumulativeCount);
                update.setInt(4, row.id());
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private boolean hasNullRollingStatistics(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + tableName
                                + " WHERE rollingAverage IS NULL "
                                + "OR rollingStandardDeviation IS NULL")) {
            result.next();
            return result.getInt(1) > 0;
        }
    }

    private boolean hasUncorrectedSampleVariance(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + tableName
                                + " WHERE sampleVarianceCorrected = FALSE")) {
            result.next();
            return result.getInt(1) > 0;
        }
    }

    private void dropDateUniqueness(Connection connection, String tableName) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        if ("H2".equalsIgnoreCase(productName)) {
            dropH2DateConstraint(connection, tableName);
            return;
        }

        String indexName = findSingleColumnDateIndex(connection, tableName);
        if (indexName == null) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + quote(connection, tableName)
                            + " DROP INDEX " + quote(connection, indexName));
        }
    }

    private void dropH2DateConstraint(Connection connection, String tableName) throws SQLException {
        String sql = """
                SELECT tc.CONSTRAINT_NAME
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu
                  ON tc.CONSTRAINT_CATALOG = ccu.CONSTRAINT_CATALOG
                 AND tc.CONSTRAINT_SCHEMA = ccu.CONSTRAINT_SCHEMA
                 AND tc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME
                WHERE UPPER(tc.TABLE_NAME) = UPPER(?)
                  AND tc.CONSTRAINT_TYPE = 'UNIQUE'
                GROUP BY tc.CONSTRAINT_NAME
                HAVING COUNT(*) = 1 AND MAX(UPPER(ccu.COLUMN_NAME)) = 'DATE'
                """;
        String constraintName = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    constraintName = result.getString(1);
                }
            }
        }
        if (constraintName == null) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + tableName
                            + " DROP CONSTRAINT " + quote(connection, constraintName));
        }
    }

    private String findSingleColumnDateIndex(Connection connection, String tableName) throws SQLException {
        Map<String, List<String>> columnsByIndex = new LinkedHashMap<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(
                connection.getCatalog(), null, tableName, true, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                String columnName = indexes.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    columnsByIndex.computeIfAbsent(indexName, ignored -> new ArrayList<>()).add(columnName);
                }
            }
        }
        return columnsByIndex.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .filter(entry -> "date".equalsIgnoreCase(entry.getValue().getFirst()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
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

    private String quote(Connection connection, String identifier) throws SQLException {
        String quote = connection.getMetaData().getIdentifierQuoteString().trim();
        if (quote.isEmpty()) {
            return identifier;
        }
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    private record LegacySentimentRow(int id, double sentimentScore) {
    }
}
