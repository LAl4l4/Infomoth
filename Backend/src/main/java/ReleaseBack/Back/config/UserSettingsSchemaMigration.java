package ReleaseBack.Back.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Adds currency columns to user_settings databases created before the
 * two-sided exchange preference was introduced.
 */
@Component
@DependsOn("dataSourceScriptDatabaseInitializer")
public class UserSettingsSchemaMigration implements InitializingBean {
    private static final String TABLE_NAME = "user_settings";

    private final DataSource dataSource;

    public UserSettingsSchemaMigration(@Qualifier("dataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        migrate();
    }

    public void migrate() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            addColumnIfMissing(
                    connection,
                    "default_base_currency",
                    "VARCHAR(10) NOT NULL DEFAULT 'USD'");
            addColumnIfMissing(
                    connection,
                    "default_quote_currency",
                    "VARCHAR(10) NOT NULL DEFAULT 'CNY'");
        }
    }

    private void addColumnIfMissing(Connection connection, String columnName, String columnDefinition)
            throws SQLException {
        if (hasColumn(connection.getMetaData(), columnName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private boolean hasColumn(DatabaseMetaData metadata, String columnName) throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, null, null, null)) {
            while (columns.next()) {
                if (TABLE_NAME.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
