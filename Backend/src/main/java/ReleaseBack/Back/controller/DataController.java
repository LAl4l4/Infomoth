package ReleaseBack.Back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import ReleaseBack.Back.service.DataService;
import ReleaseBack.Back.service.MarketTrendService;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import ReleaseBack.Back.DTO.aiSkillDTO;
import ReleaseBack.Back.DTO.MarketTrendDTO;
import ReleaseBack.Back.DTO.usStockIndexDTO;

@RequiredArgsConstructor
@RestController
@RequestMapping("/data")
public class DataController {
    
    private final DataService dataService;
    private final MarketTrendService marketTrendService;

    @GetMapping("/exchangerate")
    public ResponseEntity<Double> getExchangeRate(
        @RequestParam String base, 
        @RequestParam String quote) {
        double rate = dataService.getExchangeRate(base, quote);
        return ResponseEntity.ok(rate);
    }

    @GetMapping("/currencies")
    public ResponseEntity<Set<String>> getCurrencies() {
        return ResponseEntity.ok(dataService.getCurrencies());
    }

    @GetMapping("/ai-skills")
    public ResponseEntity<List<aiSkillDTO>> getPopularAISkills() {
        return ResponseEntity.ok(dataService.getPopularAISkills());
    }

    @GetMapping("/sentiment")
    public ResponseEntity<Double> getSentimentScore() {
        return ResponseEntity.ok(dataService.getSentimentScore());
    }

    @GetMapping("/us-stock-indices")
    public ResponseEntity<List<usStockIndexDTO>> getUsStockIndices() {
        return ResponseEntity.ok(dataService.getUsStockIndices());
    }

    @GetMapping("/market-trends")
    public ResponseEntity<MarketTrendDTO> getMarketTrends() {
        return ResponseEntity.ok(marketTrendService.getLastSevenDays());
    }
}
