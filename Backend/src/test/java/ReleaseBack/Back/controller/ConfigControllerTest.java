package ReleaseBack.Back.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ConfigControllerTest {

    @Mock
    private HttpServletRequest request;

    private final ConfigController configController = new ConfigController();

    @Test
    void getBaseUrlShouldOmitDefaultHttpPort() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(80);

        ResponseEntity<Map<String, String>> response = configController.getBaseUrl(request);

        Map<String, String> body = response.getBody();

        if (body == null) {
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body is null");
            throw new AssertionError("Response body is null");
        }

        assertEquals(200, response.getStatusCode().value());
        assertEquals("http://localhost", body.get("baseUrl"));
    }

    @Test
    void getBaseUrlShouldOmitDefaultHttpsPort() {
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("api.infomoth.com");
        when(request.getServerPort()).thenReturn(443);

        ResponseEntity<Map<String, String>> response = configController.getBaseUrl(request);

        Map<String, String> body = response.getBody();

        if (body == null) {
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body is null");
            throw new AssertionError("Response body is null");
        }

        assertEquals("https://api.infomoth.com", body.get("baseUrl"));
    }

    @Test
    void getBaseUrlShouldIncludeCustomPort() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);

        ResponseEntity<Map<String, String>> response = configController.getBaseUrl(request);

        Map<String, String> body = response.getBody();

        if (body == null) {
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body is null");
            throw new AssertionError("Response body is null");
        }

        assertTrue(body.containsKey("baseUrl"));
        assertEquals("http://localhost:8080", body.get("baseUrl"));
    }
}
