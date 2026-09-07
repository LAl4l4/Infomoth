package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import ReleaseBack.Back.mapper.MarketInputMapper;
import ReleaseBack.Back.mapper.MarketSignalMapper;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {"spring.sql.init.mode=never", "mybatis.mapper-locations=classpath*:mapper/*.xml"})
class MarketInputPersistenceTest {
    @Autowired MarketInputMapper mapper;
    @Autowired MarketSignalMapper state;
    @Autowired JdbcTemplate jdbc;
    @TempDir Path directory;
    MarketInputPersistenceService service;

    @BeforeEach
    void setup() throws Exception {
        // Execute the production schema so column drift cannot hide behind a test-only table.
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
            new org.springframework.core.io.FileSystemResource("../Schema/09-market-inputs.sql"),
            new org.springframework.core.io.FileSystemResource("../Schema/10-market-signal-state.sql"))
            .execute(jdbc.getDataSource());
        jdbc.execute("DELETE FROM market_observation");
        jdbc.execute("DELETE FROM market_input_source");
        jdbc.execute("UPDATE market_signal_state SET input_sha = '', signature = NULL, payload = NULL WHERE id = 1");
        service = new MarketInputPersistenceService(directory, mapper, state);
    }

    @Test
    void repeatedImportsAreIdempotentAndPartialFailuresKeepHistory() throws Exception {
        write(18, "2026-09-04T20:30:00Z", "ok");
        service.ingest();
        service.ingest();
        var first = mapper.findObservation("vix", LocalDate.parse("2026-09-04"));
        assertEquals(18, first.getValue());
        assertTrue(first.getAvailableAt() >= first.getFetchedAt());
        assertEquals(1, mapper.findSince(LocalDate.parse("2026-01-01")).size());
        write(18, "2026-09-04T21:30:00Z", "error");
        service.ingest();
        var refreshed = mapper.findObservation("vix", first.getDate());
        assertEquals(first.getAvailableAt(), refreshed.getAvailableAt());
        assertEquals(Instant.parse("2026-09-04T21:30:00Z").toEpochMilli(), refreshed.getFetchedAt());
        assertEquals("error", mapper.findSources().getFirst().getStatus());
        Files.writeString(directory.resolve("market_inputs.json"), "{broken");
        service.ingest();
        assertEquals(18, mapper.findObservation("vix", first.getDate()).getValue());
    }

    @Test
    void persistedFingerprintSkipsObservationQueriesAfterRestart() throws Exception {
        write(18, "2026-09-04T20:30:00Z", "ok");
        service.ingest();
        var observedMapper = org.mockito.Mockito.spy(mapper);
        new MarketInputPersistenceService(directory, observedMapper, state).ingest();
        org.mockito.Mockito.verifyNoInteractions(observedMapper);
        assertEquals(64, state.lockInputHash().length());
    }

    @Test
    void correctionsUpdateValuesAndOlderSnapshotsCannotOverwriteThem() throws Exception {
        write(18, "2026-09-04T20:30:00Z", "ok");
        service.ingest();
        write(19, "2026-09-04T21:30:00Z", "ok");
        service.ingest();
        write(17, "2026-09-04T19:30:00Z", "error");
        service.ingest();
        assertEquals(19, mapper.findObservation("vix", LocalDate.parse("2026-09-04")).getValue());
        assertEquals("ok", mapper.findSources().getFirst().getStatus());
    }

    @Test
    void rejectsUnknownIndicatorFutureDatesAndStringNumbers() throws Exception {
        Files.writeString(directory.resolve("market_inputs.json"), """
            {"observations":[
              {"indicator":"unknown","date":"2026-09-04","value":1,"source":"fred","fetchedAt":"2026-09-04T20:30:00Z"},
              {"indicator":"vix","date":"2099-09-04","value":18,"source":"fred","fetchedAt":"2026-09-04T20:30:00Z"},
              {"indicator":"vix","date":"2026-09-04","value":"18","source":"fred","fetchedAt":"2026-09-04T20:30:00Z"}
            ],"sources":[]}
            """);
        service.ingest();
        assertTrue(mapper.findSince(LocalDate.of(2000, 1, 1)).isEmpty());
    }

    private void write(double value, String fetchedAt, String status) throws Exception {
        Files.writeString(directory.resolve("market_inputs.json"), """
            {"observations":[{"indicator":"vix","date":"2026-09-04","value":%s,"source":"fred","fetchedAt":"%s"}],
             "sources":[{"source":"fred","status":"%s","message":"","attemptedAt":"%s"}]}
            """.formatted(value, fetchedAt, status, fetchedAt));
    }
}
