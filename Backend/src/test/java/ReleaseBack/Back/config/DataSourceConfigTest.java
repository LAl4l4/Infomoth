package ReleaseBack.Back.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DataSourceConfigTest {

    @Test
    void buildsMysqlUrlFromConfigConnection() {
        AppConfigProvider.MysqlConnection mysql = new AppConfigProvider.MysqlConnection(
                "db.internal", 3307, "infomoth", "secret");

        assertEquals(
                "jdbc:mysql://db.internal:3307/infomoth?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC",
                DataSourceConfig.buildMysqlJdbcUrl(mysql, "infomoth"));
    }
}
