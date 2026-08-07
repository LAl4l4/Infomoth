package ReleaseBack.Back.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import ReleaseBack.Back.entity.UsStockIndexRecord;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:us_stock_mapper_db;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "mybatis.mapper-locations=classpath*:mapper/*.xml"
})
class UsStockIndexMapperTest {

    @Autowired
    private UsStockIndexMapper stockIndexMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS us_stock_indices (
                    ID INT AUTO_INCREMENT PRIMARY KEY,
                    symbol VARCHAR(32) NOT NULL,
                    name VARCHAR(128) NOT NULL,
                    price DOUBLE NOT NULL,
                    change_value DOUBLE NOT NULL,
                    change_percent DOUBLE NOT NULL,
                    date DATE NOT NULL,
                    source VARCHAR(128),
                    UNIQUE (symbol, date)
                )
                """
        );
        jdbcTemplate.execute("DELETE FROM us_stock_indices");
    }

    @Test
    void shouldInsertUpdateAndFindDailySnapshot() {
        LocalDate date = LocalDate.of(2026, 8, 8);
        UsStockIndexRecord record = record(date, 100.0);

        assertEquals(0, stockIndexMapper.updateSnapshot(record));
        assertEquals(1, stockIndexMapper.insertSnapshot(record));

        record.setPrice(101.5);
        record.setChangeValue(1.5);
        assertEquals(1, stockIndexMapper.updateSnapshot(record));

        List<UsStockIndexRecord> results = stockIndexMapper.findSince(date);
        assertEquals(1, results.size());
        assertEquals(101.5, results.get(0).getPrice());
        assertEquals(1.5, results.get(0).getChangeValue());
    }

    private UsStockIndexRecord record(LocalDate date, double price) {
        UsStockIndexRecord record = new UsStockIndexRecord();
        record.setSymbol("^GSPC");
        record.setName("S&P 500");
        record.setPrice(price);
        record.setChangeValue(1.0);
        record.setChangePercent(0.1);
        record.setDate(date);
        record.setSource("test");
        return record;
    }
}
