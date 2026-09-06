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
        "spring.datasource.url=jdbc:h2:mem:sentiment_file_state_mapper_db;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "mybatis.mapper-locations=classpath*:mapper/*.xml"
})
class SentimentFileStateMapperTest {

    @Autowired
    private SentimentFileStateMapper stateMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sentiment_file_state ("
                        + "ID TINYINT PRIMARY KEY, politicsSha256 CHAR(64), techSha256 CHAR(64), "
                        + "CHECK (ID = 1))");
        jdbcTemplate.execute("DELETE FROM sentiment_file_state");
    }

    @Test
    void storesBothHashesInTheSingleCheckpointRow() {
        assertEquals(1, stateMapper.insert());
        assertEquals(1, stateMapper.updatePoliticsSha256("a".repeat(64)));
        assertEquals(1, stateMapper.updateTechSha256("b".repeat(64)));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sentiment_file_state", Integer.class));
        assertEquals("a".repeat(64), stateMapper.find().getPoliticsSha256());
        assertEquals("b".repeat(64), stateMapper.find().getTechSha256());
    }

    @Test
    void returnsNullUntilTheCheckpointRowIsCreated() {
        assertNull(stateMapper.find());
    }
}
