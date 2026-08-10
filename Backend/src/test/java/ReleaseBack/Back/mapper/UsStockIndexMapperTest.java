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
                    date DATE NOT NULL,
                    sp500_price DOUBLE NOT NULL,
                    sp500_change_percent DOUBLE NOT NULL,
                    dow_jones_price DOUBLE NOT NULL,
                    dow_jones_change_percent DOUBLE NOT NULL,
                    nasdaq_price DOUBLE NOT NULL,
                    nasdaq_change_percent DOUBLE NOT NULL,
                    russell_2000_price DOUBLE NOT NULL,
                    russell_2000_change_percent DOUBLE NOT NULL,
                    UNIQUE (date)
                )
                """
        );
        jdbcTemplate.execute("DELETE FROM us_stock_indices");
    }

    @Test
    void shouldInsertUpdateAndFindDailySnapshot() {
        LocalDate date = LocalDate.of(2026, 8, 8);
        UsStockIndexRecord record = record(date, 0.1);

        assertEquals(0, stockIndexMapper.updateDailyChange(record));
        assertEquals(1, stockIndexMapper.insertDailyChange(record));

        record.setSp500Price(5100.0);
        record.setSp500ChangePercent(0.2);
        assertEquals(1, stockIndexMapper.updateDailyChange(record));

        List<UsStockIndexRecord> results = stockIndexMapper.findSince(date);
        assertEquals(1, results.size());
        assertEquals(5100.0, results.get(0).getSp500Price());
        assertEquals(0.2, results.get(0).getSp500ChangePercent());
        assertEquals(-0.1, results.get(0).getRussell2000ChangePercent());
    }

    private UsStockIndexRecord record(LocalDate date, double sp500ChangePercent) {
        UsStockIndexRecord record = new UsStockIndexRecord();
        record.setDate(date);
        record.setSp500Price(5000.0);
        record.setSp500ChangePercent(sp500ChangePercent);
        record.setDowJonesPrice(40000.0);
        record.setDowJonesChangePercent(0.05);
        record.setNasdaqPrice(16000.0);
        record.setNasdaqChangePercent(0.3);
        record.setRussell2000Price(2000.0);
        record.setRussell2000ChangePercent(-0.1);
        return record;
    }
}
