package ReleaseBack.Back.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ReleaseBack.Back.service.MarketSignalService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class MarketSignalController {
    private final MarketSignalService service;

    @GetMapping(value = "/market-signal", produces = "application/json")
    public String getSignal() {
        return service.getSignal();
    }
}
