package ReleaseBack.Back.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import ReleaseBack.Back.DTO.aiSkillDTO;
import ReleaseBack.Back.DTO.MarketTrendDTO;
import ReleaseBack.Back.DTO.SentimentScoreDTO;
import ReleaseBack.Back.DTO.usStockIndexDTO;
import ReleaseBack.Back.service.DataService;
import ReleaseBack.Back.service.MarketTrendService;

@ExtendWith(MockitoExtension.class)
class DataControllerTest {

    @Mock
    private DataService dataService;

    @Mock
    private MarketTrendService marketTrendService;

    @InjectMocks
    private DataController dataController;

    @Test
    void getExchangeRateShouldReturnServiceValue() {
        when(dataService.getExchangeRate("USD", "AUD")).thenReturn(1.52);

        ResponseEntity<Double> response = dataController.getExchangeRate("USD", "AUD");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1.52, response.getBody());
    }

    @Test
    void getCurrenciesShouldReturnServiceValue() {
        Set<String> expected = Set.of("USD", "AUD");
        when(dataService.getCurrencies()).thenReturn(expected);

        ResponseEntity<Set<String>> response = dataController.getCurrencies();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getPopularAISkillsShouldReturnServiceValue() {
        aiSkillDTO dto = new aiSkillDTO();
        dto.setRank(1);
        dto.setSkill("RAG");
        dto.setMentions(10);
        List<aiSkillDTO> expected = List.of(dto);
        when(dataService.getPopularAISkills()).thenReturn(expected);

        ResponseEntity<List<aiSkillDTO>> response = dataController.getPopularAISkills();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getSentimentScoreShouldReturnServiceValue() {
        SentimentScoreDTO expected = new SentimentScoreDTO(0.42, 0.31);
        when(dataService.getSentimentScore()).thenReturn(expected);

        ResponseEntity<SentimentScoreDTO> response = dataController.getSentimentScore();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getUsStockIndicesShouldReturnServiceValue() {
        usStockIndexDTO dto = new usStockIndexDTO();
        dto.setSymbol("^GSPC");
        dto.setName("S&P 500");
        dto.setPrice(7483.23);
        dto.setChange(-16.13);
        dto.setChangePercent(-0.22);
        List<usStockIndexDTO> expected = List.of(dto);
        when(dataService.getUsStockIndices()).thenReturn(expected);

        ResponseEntity<List<usStockIndexDTO>> response = dataController.getUsStockIndices();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getMarketTrendsShouldReturnServiceValue() {
        MarketTrendDTO expected = new MarketTrendDTO(List.of(), List.of());
        when(marketTrendService.getMarketTrends()).thenReturn(expected);

        ResponseEntity<MarketTrendDTO> response = dataController.getMarketTrends();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }
}
