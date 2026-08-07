package ReleaseBack.Back.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ReleaseBack.Back.DTO.SettingsDTO;
import ReleaseBack.Back.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/general")
    public SettingsDTO getGeneralSettings(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return new SettingsDTO(
                settingsService.getDefaultPage(userId),
                settingsService.getDefaultBaseCurrency(userId),
                settingsService.getDefaultQuoteCurrency(userId));
    }

    @PutMapping("/general")
    public SettingsDTO updateGeneralSettings(
            @RequestBody SettingsDTO settingsDTO,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (settingsDTO.getDefaultBaseCurrency() == null
                && settingsDTO.getDefaultQuoteCurrency() == null) {
            int defaultPage = settingsService.updateDefaultPage(
                    userId,
                    settingsDTO.getDefaultPage());
            return new SettingsDTO(
                    defaultPage,
                    settingsService.getDefaultBaseCurrency(userId),
                    settingsService.getDefaultQuoteCurrency(userId));
        }
        return settingsService.updateSettings(
                userId,
                settingsDTO.getDefaultPage(),
                settingsDTO.getDefaultBaseCurrency(),
                settingsDTO.getDefaultQuoteCurrency());
    }
}
