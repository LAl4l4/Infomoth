package ReleaseBack.Back.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/** Adds the rolling-average sample count to sentiment tables created by older releases. */
@Component
@DependsOn("dataSourceScriptDatabaseInitializer")
public class SentimentSchemaMigration implements InitializingBean {
    private static final String[] TABLE_NAMES = {"politics_average", "tech_average"};
    private static final String COLUMN_NAME = "sampleCount";

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
                addColumnIfMissing(connection, tableName);
            }
        }
    }

    private void addColumnIfMissing(Connection connection, String tableName) throws SQLException {
        if (hasColumn(connection.getMetaData(), tableName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_NAME + " INT NOT NULL DEFAULT 1");
        }
    }

    private boolean hasColumn(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, null, null, null)) {
            while (columns.next()) {
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && COLUMN_NAME.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
