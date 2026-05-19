package ReleaseBack.Back.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @GetMapping("/baseurl")
    public ResponseEntity<Map<String, String>> getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();

        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        String baseUrl = defaultPort
                ? scheme + "://" + host
                : scheme + "://" + host + ":" + port;

        return ResponseEntity.ok(Map.of("baseUrl", baseUrl));
    }
}
