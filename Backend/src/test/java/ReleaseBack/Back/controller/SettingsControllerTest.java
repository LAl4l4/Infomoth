package ReleaseBack.Back.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ReleaseBack.Back.DTO.SettingsDTO;
import ReleaseBack.Back.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock
    private SettingsService settingsService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SettingsController settingsController;

    @Test
    void getGeneralSettingsShouldUseAuthenticatedUserId() {
        when(request.getAttribute("userId")).thenReturn(12);
        when(settingsService.getDefaultPage(12)).thenReturn(3);
        when(settingsService.getDefaultBaseCurrency(12)).thenReturn("USD");
        when(settingsService.getDefaultQuoteCurrency(12)).thenReturn("CNY");

        SettingsDTO result = settingsController.getGeneralSettings(request);

        assertEquals(3, result.getDefaultPage());
        assertEquals("USD", result.getDefaultBaseCurrency());
        assertEquals("CNY", result.getDefaultQuoteCurrency());
    }

    @Test
    void updateGeneralSettingsShouldPersistForAuthenticatedUser() {
        when(request.getAttribute("userId")).thenReturn(12);
        when(settingsService.updateDefaultPage(12, 4)).thenReturn(4);
        when(settingsService.getDefaultBaseCurrency(12)).thenReturn("USD");
        when(settingsService.getDefaultQuoteCurrency(12)).thenReturn("CNY");

        SettingsDTO result = settingsController.updateGeneralSettings(
                new SettingsDTO(4),
                request);

        assertEquals(4, result.getDefaultPage());
        assertEquals("USD", result.getDefaultBaseCurrency());
        assertEquals("CNY", result.getDefaultQuoteCurrency());
        verify(settingsService).updateDefaultPage(12, 4);
    }

    @Test
    void updateGeneralSettingsShouldPersistBothCurrencySides() {
        when(request.getAttribute("userId")).thenReturn(12);
        SettingsDTO expected = new SettingsDTO(2, "EUR", "AUD");
        when(settingsService.updateSettings(12, 2, "EUR", "AUD")).thenReturn(expected);

        SettingsDTO result = settingsController.updateGeneralSettings(
                new SettingsDTO(2, "EUR", "AUD"),
                request);

        assertEquals("EUR", result.getDefaultBaseCurrency());
        assertEquals("AUD", result.getDefaultQuoteCurrency());
        verify(settingsService).updateSettings(12, 2, "EUR", "AUD");
    }
}
