package ReleaseBack.Back.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/** Ensures the one-row sentiment file checkpoint exists after schema initialization. */
@Component
@DependsOn("dataSourceScriptDatabaseInitializer")
public class SentimentFileStateSchemaMigration implements InitializingBean {
    private final DataSource dataSource;

    public SentimentFileStateSchemaMigration(@Qualifier("dataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        ensureRow();
    }

    public void ensureRow() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean missing;
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT COUNT(*) FROM sentiment_file_state WHERE ID = 1")) {
                result.next();
                missing = result.getInt(1) == 0;
            }
            if (missing) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO sentiment_file_state (ID) VALUES (1)");
                }
            }
        }
    }
}
