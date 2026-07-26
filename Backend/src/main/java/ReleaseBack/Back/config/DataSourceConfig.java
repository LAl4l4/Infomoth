package ReleaseBack.Back.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {
    private static final String MYSQL_URL_PREFIX = "jdbc:mysql://";

    @Bean
    public DataSource dataSource(
            AppConfigProvider appConfigProvider,
            @Value("${spring.datasource.url:}") String configuredJdbcUrl,
            @Value("${spring.datasource.username:}") String configuredUsername,
            @Value("${spring.datasource.password:}") String configuredPassword,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        AppConfigProvider.MysqlConnection mysql = appConfigProvider.getMysqlConnection();
        boolean usesConfiguredMysql = configuredJdbcUrl.isBlank() || configuredJdbcUrl.startsWith(MYSQL_URL_PREFIX);
        String jdbcUrl = usesConfiguredMysql
                ? buildMysqlJdbcUrl(mysql, appConfigProvider.getDatabaseName())
                : configuredJdbcUrl;

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(usesConfiguredMysql ? mysql.user() : configuredUsername)
                .password(usesConfiguredMysql ? mysql.password() : configuredPassword)
                .driverClassName(driverClassName)
                .build();
    }

    static String buildMysqlJdbcUrl(AppConfigProvider.MysqlConnection mysql, String databaseName) {
        return MYSQL_URL_PREFIX + mysql.host() + ":" + mysql.port() + "/" + databaseName
                + "?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC";
    }

}
