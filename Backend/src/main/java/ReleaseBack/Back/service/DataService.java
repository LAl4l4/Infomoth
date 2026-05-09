package ReleaseBack.Back.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ReleaseBack.Back.DTO.aiSkillDTO;
import ReleaseBack.Back.DTO.exchangeRateDTO;

import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.nio.file.Path;
import java.io.File;
import java.util.Set;


@Service
public class DataService {
    private final ObjectMapper mapper = new ObjectMapper();

    private List<exchangeRateDTO> loadAllRates() {
        File jsonFile = resolveCrawlerFile("exchangeRates.json");
        try {
            return mapper.readValue(
                jsonFile,
                new TypeReference<List<exchangeRateDTO>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Error reading JSON stream: " + e.getMessage(), e);
        }
    }

    private File resolveCrawlerFile(String fileName) {
        Path path = Paths.get(
            System.getProperty("user.dir"),
            "..", "Crawler", fileName
        );
        File jsonFile = path.toFile();
        if (!jsonFile.exists()) {
            throw new RuntimeException("Data file not found: " + jsonFile.getAbsolutePath());
        }
        return jsonFile;
    }
    
    public double getExchangeRate(String base, String quote) {
        List<exchangeRateDTO> allRates = loadAllRates();

        for (exchangeRateDTO rate : allRates) {
            if (rate.getBase().equals(base) && rate.getQuote().equals(quote)) {
                return rate.getRate();
            }
        }

        throw new RuntimeException("Exchange rate not found for " + base + "/" + quote);

    
    }

    public Set<String> getCurrencies() {
        List<exchangeRateDTO> allRates = loadAllRates();
        Set<String> currencies = new LinkedHashSet<>();

        for (exchangeRateDTO rate : allRates) {
            if (rate.getBase() != null) {
                currencies.add(rate.getBase());
            }
            if (rate.getQuote() != null) {
                currencies.add(rate.getQuote());
            }
        }

        return currencies;
    }

    public List<aiSkillDTO> getPopularAISkills() {
        File jsonFile = resolveCrawlerFile("ai_skills_today.json");
        try {
            return mapper.readValue(
                jsonFile,
                new TypeReference<List<aiSkillDTO>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Error reading AI skills JSON stream: " + e.getMessage(), e);
        }
    }
}
