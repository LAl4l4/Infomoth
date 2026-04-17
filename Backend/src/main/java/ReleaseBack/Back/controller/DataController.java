package ReleaseBack.Back.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ReleaseBack.Back.service.DataService;
import java.util.Set;

@RestController
@RequestMapping("/data")
@CrossOrigin(origins = "http://localhost:3000") 
public class DataController {
    
    @Autowired
    private DataService dataService;

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
}
