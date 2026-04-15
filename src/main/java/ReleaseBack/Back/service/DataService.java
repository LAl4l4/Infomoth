package ReleaseBack.Back.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ReleaseBack.Back.DTO.exchangeRateDTO;

import java.nio.file.Paths;
import java.util.List;
import java.nio.file.Path;
import java.io.File;


@Service
public class DataService {
    
    public double getExchangeRate(String base, String quote) {

        Path path = Paths.get(
            System.getProperty("user.dir"), 
            "..", "Crawler", "exchangeRates.json"
        );
        File jsonFile = path.toFile();

        if (!jsonFile.exists()) {
            throw new RuntimeException("Data file not found: " + jsonFile.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        List<exchangeRateDTO> allRates;
        try{
            allRates = mapper.readValue(
                jsonFile, 
                new TypeReference<List<exchangeRateDTO>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Error reading JSON stream: " + e.getMessage(), e);
        } 

        for (exchangeRateDTO rate : allRates) {
            if (rate.getBase().equals(base) && rate.getQuote().equals(quote)) {
                return rate.getRate();
            }
        }

        throw new RuntimeException("Exchange rate not found for " + base + "/" + quote);

    
    }
}
