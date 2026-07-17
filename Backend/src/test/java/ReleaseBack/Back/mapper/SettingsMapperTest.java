package ReleaseBack.Back.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:settings_mapper_db;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class SettingsMapperTest {

    @Autowired
    private SettingsMapper settingsMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS user_settings (
                    user_id INT PRIMARY KEY,
                    default_page TINYINT NOT NULL DEFAULT 0
                )
                """
        );
        jdbcTemplate.execute("DELETE FROM user_settings");
    }

    @Test
    void defaultPageShouldInsertAndUpdateByUserId() {
        assertNull(settingsMapper.findDefaultPageByUserId(9));

        settingsMapper.insertDefaultPage(9, 2);
        assertEquals(2, settingsMapper.findDefaultPageByUserId(9));

        settingsMapper.updateDefaultPage(9, 4);
        assertEquals(4, settingsMapper.findDefaultPageByUserId(9));
    }
}
