package ReleaseBack.Back.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ReleaseBack.Back.DTO.aiSkillDTO;
import ReleaseBack.Back.DTO.exchangeRateDTO;
import ReleaseBack.Back.DTO.usStockIndexDTO;
import ReleaseBack.Back.config.AppConfigProvider;
import ReleaseBack.Back.entity.SentimentAverage;
import ReleaseBack.Back.exception.DataFileNotFoundException;
import ReleaseBack.Back.exception.DataReadException;
import ReleaseBack.Back.mapper.SentimentMapper;

import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.nio.file.Path;
import java.io.File;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;


@Service
public class DataService {
    private final ObjectMapper mapper = new ObjectMapper();

    private final SentimentMapper sentimentMapper;

    private final Path sharedDir;

    // production Shared directory comes from Config/app-config.json
    @Autowired
    public DataService(AppConfigProvider appConfigProvider, SentimentMapper sentimentMapper) {
        this(Path.of(appConfigProvider.getSharedDirectory()), sentimentMapper);
    }

    // only used for testing, allows injection of a custom Shared directory path
    public DataService(Path sharedDir, SentimentMapper sentimentMapper) {
        this.sentimentMapper = sentimentMapper;
        this.sharedDir = sharedDir;
    }

    private List<exchangeRateDTO> loadAllRates() {
        File jsonFile = resolveSharedFile("exchangeRates.json");
        try {
            return mapper.readValue(
                jsonFile,
                new TypeReference<List<exchangeRateDTO>>() {}
            );
        } catch (Exception e) {
            throw new DataReadException("Error reading JSON stream: " + e.getMessage());
        }
    }

    private File resolveSharedFile(String fileName) {
        Path path = Paths.get(sharedDir.toString(), fileName);
        File jsonFile = path.toFile();
        if (!jsonFile.exists()) {
            throw new DataFileNotFoundException(jsonFile.getAbsolutePath());
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

        throw new DataFileNotFoundException("Exchange rate not found for " + base + "/" + quote);

    
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

    public double getSentimentScore() {
        SentimentAverage politics = sentimentMapper.findLatestByTable("politics_average");
        SentimentAverage tech = sentimentMapper.findLatestByTable("tech_average");

        if (politics == null && tech == null) {
            throw new DataFileNotFoundException("No sentiment data available");
        }
        if (politics == null) {
            return tech.getSentimentScore();
        }
        if (tech == null) {
            return politics.getSentimentScore();
        }
        return (politics.getSentimentScore() + tech.getSentimentScore()) / 2.0;
    }

    public List<aiSkillDTO> getPopularAISkills() {
        File jsonFile = resolveSharedFile("ai_skills_today.json");
        try {
            return mapper.readValue(
                jsonFile,
                new TypeReference<List<aiSkillDTO>>() {}
            );
        } catch (Exception e) {
            throw new DataReadException("Error reading AI skills JSON stream: " + e.getMessage());
        }
    }

    public List<usStockIndexDTO> getUsStockIndices() {
        File jsonFile = resolveSharedFile("us_stock_indices.json");
        try {
            return mapper.readValue(
                jsonFile,
                new TypeReference<List<usStockIndexDTO>>() {}
            );
        } catch (Exception e) {
            throw new DataReadException("Error reading US stock indices JSON stream: " + e.getMessage());
        }
    }
}
