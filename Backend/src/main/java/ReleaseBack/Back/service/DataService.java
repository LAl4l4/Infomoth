package ReleaseBack.Back.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ReleaseBack.Back.DTO.aiSkillDTO;
import ReleaseBack.Back.DTO.exchangeRateDTO;
import ReleaseBack.Back.DTO.SentimentScoreDTO;
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
import java.io.IOException;
import java.time.LocalDate;
import java.util.OptionalDouble;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;


@Service
public class DataService {
    private static final String POLITICS_FILE = "politics_news.json";
    private static final String TECH_FILE = "tech_news.json";

    private final ObjectMapper mapper = new ObjectMapper();

    private final SentimentMapper sentimentMapper;
    private final SentimentFileReader sentimentFileReader;

    private final Path sharedDir;

    // production Shared directory comes from Config/app-config.json
    @Autowired
    public DataService(
            AppConfigProvider appConfigProvider,
            SentimentMapper sentimentMapper,
            SentimentFileReader sentimentFileReader) {
        this(appConfigProvider.getSharedDirectory(), sentimentMapper, sentimentFileReader);
    }

    // only used for testing, allows injection of a custom Shared directory path
    public DataService(Path sharedDir, SentimentMapper sentimentMapper) {
        this(sharedDir, sentimentMapper, new SentimentFileReader(new ObjectMapper()));
    }

    DataService(Path sharedDir, SentimentMapper sentimentMapper, SentimentFileReader sentimentFileReader) {
        this.sentimentMapper = sentimentMapper;
        this.sentimentFileReader = sentimentFileReader;
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

    public SentimentScoreDTO getSentimentScore() {
        Double instant = averageAvailable(
                readCurrentAverage(POLITICS_FILE),
                readCurrentAverage(TECH_FILE));

        LocalDate today = LocalDate.now();
        Double dailyAverage = averageAvailable(
                scoreOf(sentimentMapper.findByDate("politics_average", today)),
                scoreOf(sentimentMapper.findByDate("tech_average", today)));

        return new SentimentScoreDTO(instant, dailyAverage);
    }

    private Double readCurrentAverage(String fileName) {
        try {
            OptionalDouble average = sentimentFileReader.readAverage(sharedDir.resolve(fileName));
            return average.isPresent() ? average.getAsDouble() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private Double scoreOf(SentimentAverage average) {
        if (average == null || average.getSentimentScore() == null
                || !Double.isFinite(average.getSentimentScore())) {
            return null;
        }
        return average.getSentimentScore();
    }

    private Double averageAvailable(Double... values) {
        double total = 0;
        int count = 0;
        for (Double value : values) {
            if (value != null && Double.isFinite(value)) {
                total += value;
                count++;
            }
        }
        return count == 0 ? null : total / count;
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
