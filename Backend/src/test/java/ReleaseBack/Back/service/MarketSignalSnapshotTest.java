package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import ReleaseBack.Back.exception.BaseException;
import ReleaseBack.Back.DTO.MarketSignalDTO;
import ReleaseBack.Back.entity.MarketObservation;
import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.mapper.MarketInputMapper;
import ReleaseBack.Back.mapper.MarketSignalMapper;
import ReleaseBack.Back.mapper.SentimentMapper;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {"spring.sql.init.mode=never", "mybatis.mapper-locations=classpath*:mapper/*.xml"})
class MarketSignalSnapshotTest {
    @Autowired MarketInputMapper inputs;
    @Autowired MarketSignalMapper snapshots;
    @Autowired JdbcTemplate jdbc;
    final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    final Instant now = Instant.parse("2026-09-07T12:00:00Z");
    SentimentMapper sentiments;
    LinearMarketPredictionModel model;
    MarketSignalSnapshotService producer;

    @BeforeEach
    void setup() {
        new ResourceDatabasePopulator(new FileSystemResource("../Schema/09-market-inputs.sql"),
            new FileSystemResource("../Schema/10-market-signal-state.sql")).execute(jdbc.getDataSource());
        jdbc.execute("DELETE FROM market_observation");
        jdbc.execute("DELETE FROM market_input_source");
        jdbc.execute("UPDATE market_signal_state SET input_sha = '', signature = NULL, payload = NULL WHERE id = 1");
        sentiments = mock(SentimentMapper.class);
        model = spy(new LinearMarketPredictionModel());
        producer = new MarketSignalSnapshotService(inputs, snapshots, sentiments, model, json);
    }

    @Test
    void boundedHistoryProducesSameResponseAsFullHistory() throws Exception {
        for (int day = 0; day < 200; day++) {
            inputs.insertObservation(observation("sp500", LocalDate.of(2026, 9, 7).minusDays(day), 5000 - day));
        }
        var recent = inputs.findRecent("sp500", LocalDate.of(2025, 8, 3), LocalDate.of(2026, 9, 7), now.toEpochMilli());
        assertEquals(60, recent.size());
        assertEquals(LocalDate.of(2026, 9, 7), recent.getFirst().getDate());
        producer.refreshAt(now);
        var expected = new MarketSignalDTO(model.predict(inputs.findSince(LocalDate.of(2025, 8, 3)), now), List.of());
        assertEquals(json.readTree(json.writeValueAsString(expected)), json.readTree(snapshots.findPayload()));
    }

    @Test
    void unchangedInputsSkipHistoryAndModelEvenAfterRestart() {
        producer.refreshAt(now);
        String payload = snapshots.findPayload();
        var observedInputs = spy(inputs);
        var restarted = new MarketSignalSnapshotService(observedInputs, snapshots, sentiments, model, json);
        restarted.refreshAt(now.plusSeconds(300));
        verify(model, times(1)).predict(anyList(), any());
        verify(observedInputs, never()).findRecent(anyString(), any(), any(), anyLong());
        assertEquals(payload, new MarketSignalService(snapshots).getSignal());
    }

    @Test
    void newsCheckpointAndModelChangesInvalidateSnapshot() {
        producer.refreshAt(now);
        SentimentAverage sample = new SentimentAverage();
        sample.setDate(LocalDate.of(2026, 9, 7));
        sample.setSentimentScore(0.8);
        sample.setRollingAverage(0.4);
        sample.setRollingStandardDeviation(0.2);
        when(sentiments.findLatestByTable("tech_average")).thenReturn(sample);
        producer.refreshAt(now.plusSeconds(300));
        snapshots.updateInputHash("new-import");
        producer.refreshAt(now.plusSeconds(600));
        doReturn("linear-mock-v2").when(model).cacheVersion();
        producer.refreshAt(now.plusSeconds(900));
        verify(model, times(4)).predict(anyList(), any());
        assertTrue(snapshots.findPayload().contains("linear-mock-v2"));
    }

    @Test
    void calendarRolloverExpiresInputsWithoutNewFiles() throws Exception {
        inputs.insertObservation(observation("vix", LocalDate.of(2026, 9, 2), 18));
        producer.refreshAt(now);
        assertEquals("available", vixStatus());
        producer.refreshAt(now.plusSeconds(86400));
        assertEquals("stale", vixStatus());
    }

    @Test
    void failedPredictionPreservesSnapshotAndRetries() {
        producer.refreshAt(now);
        String payload = snapshots.findPayload(), signature = snapshots.findSignature();
        snapshots.updateInputHash("new-import");
        doThrow(new IllegalStateException("model failed")).when(model).predict(anyList(), any());
        assertThrows(IllegalStateException.class, () -> producer.refreshAt(now.plusSeconds(300)));
        assertEquals(signature, snapshots.findSignature());
        assertEquals(payload, snapshots.findPayload());
        doCallRealMethod().when(model).predict(anyList(), any());
        producer.refreshAt(now.plusSeconds(600));
        assertNotEquals(signature, snapshots.findSignature());
    }

    @Test
    void requestReadsOnlyPersistedPayloadAndMissingSnapshotDoesNotRunModel() {
        var reader = new MarketSignalService(snapshots);
        assertEquals(503, assertThrows(BaseException.class, reader::getSignal).getCode());
        snapshots.updateSnapshot("test", "{\"prediction\":null,\"sources\":[]}");
        var observedSnapshots = spy(snapshots);
        reader = new MarketSignalService(observedSnapshots);
        assertEquals("{\"prediction\":null,\"sources\":[]}", reader.getSignal());
        verify(observedSnapshots).findPayload();
        verifyNoMoreInteractions(observedSnapshots);
        verifyNoInteractions(model, sentiments);
    }

    private String vixStatus() throws Exception {
        for (var input : json.readTree(snapshots.findPayload()).path("prediction").path("inputs")) {
            if (input.path("indicator").asText().equals("vix")) return input.path("status").asText();
        }
        throw new AssertionError("VIX input missing");
    }

    private MarketObservation observation(String code, LocalDate date, double value) {
        MarketObservation observation = new MarketObservation();
        observation.setIndicator(code);
        observation.setDate(date);
        observation.setValue(value);
        observation.setSource("fred");
        observation.setFetchedAt(now.toEpochMilli());
        observation.setAvailableAt(now.toEpochMilli());
        return observation;
    }
}
