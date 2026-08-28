package ReleaseBack.Back.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import ReleaseBack.Back.DTO.DisplaySettingsDTO;
import ReleaseBack.Back.DTO.SettingsDTO;
import ReleaseBack.Back.exception.BaseException;
import ReleaseBack.Back.mapper.SettingsMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SettingsService {

    private static final int DEFAULT_PAGE = 0;
    private static final int MAX_PAGE = 5;
    private static final String DEFAULT_BASE_CURRENCY = "USD";
    private static final String DEFAULT_QUOTE_CURRENCY = "CNY";
    private static final String DEFAULT_BACKGROUND_COLOR = "#0C101C";
    private static final String DEFAULT_GLOBE_GLOW_COLOR = "#00FFC6";
    private static final String DEFAULT_GLOBE_POINT_COLOR = "#FFFFFF";
    private static final String DEFAULT_GLOBE_MARKER_COLOR = "#00E5FF";

    private final SettingsMapper settingsMapper;

    public int getDefaultPage(Integer userId) {
        Integer defaultPage = settingsMapper.findDefaultPageByUserId(userId);
        return defaultPage == null ? DEFAULT_PAGE : defaultPage;
    }

    public String getDefaultBaseCurrency(Integer userId) {
        String defaultCurrency = settingsMapper.findDefaultBaseCurrencyByUserId(userId);
        return defaultCurrency == null || defaultCurrency.isBlank()
                ? DEFAULT_BASE_CURRENCY
                : defaultCurrency;
    }

    public String getDefaultQuoteCurrency(Integer userId) {
        String defaultCurrency = settingsMapper.findDefaultQuoteCurrencyByUserId(userId);
        return defaultCurrency == null || defaultCurrency.isBlank()
                ? DEFAULT_QUOTE_CURRENCY
                : defaultCurrency;
    }

    public DisplaySettingsDTO getDisplaySettings(Integer userId) {
        DisplaySettingsDTO settings = settingsMapper.findDisplaySettingsByUserId(userId);
        return settings == null
                ? defaultDisplaySettings()
                : settings;
    }

    @Transactional
    public int updateDefaultPage(Integer userId, Integer defaultPage) {
        validatePage(defaultPage);

        if (settingsMapper.findDefaultPageByUserId(userId) == null) {
            settingsMapper.insertDefaultPage(userId, defaultPage);
        } else {
            settingsMapper.updateDefaultPage(userId, defaultPage);
        }
        return defaultPage;
    }

    @Transactional
    public SettingsDTO updateSettings(
            Integer userId,
            Integer defaultPage,
            String defaultBaseCurrency,
            String defaultQuoteCurrency) {
        int page = defaultPage == null ? getDefaultPage(userId) : defaultPage;
        validatePage(page);
        String baseCurrency = normalizeCurrency(
                defaultBaseCurrency == null ? getDefaultBaseCurrency(userId) : defaultBaseCurrency);
        String quoteCurrency = normalizeCurrency(
                defaultQuoteCurrency == null ? getDefaultQuoteCurrency(userId) : defaultQuoteCurrency);

        if (settingsMapper.findDefaultPageByUserId(userId) == null) {
            settingsMapper.insertSettings(userId, page, baseCurrency, quoteCurrency);
        } else {
            settingsMapper.updateDefaultPage(userId, page);
            settingsMapper.updateDefaultBaseCurrency(userId, baseCurrency);
            settingsMapper.updateDefaultQuoteCurrency(userId, quoteCurrency);
        }
        return new SettingsDTO(page, baseCurrency, quoteCurrency);
    }

    @Transactional
    public DisplaySettingsDTO updateDisplaySettings(
            Integer userId,
            DisplaySettingsDTO displaySettings) {
        String backgroundColor = normalizeColor(
                displaySettings == null ? null : displaySettings.getBackgroundColor());
        String globeGlowColor = normalizeColor(
                displaySettings == null ? null : displaySettings.getGlobeGlowColor());
        String globePointColor = normalizeColor(
                displaySettings == null ? null : displaySettings.getGlobePointColor());
        String globeMarkerColor = normalizeColor(
                displaySettings == null ? null : displaySettings.getGlobeMarkerColor());

        if (settingsMapper.findDefaultPageByUserId(userId) == null) {
            settingsMapper.insertDisplaySettings(
                    userId,
                    backgroundColor,
                    globeGlowColor,
                    globePointColor,
                    globeMarkerColor);
        } else {
            settingsMapper.updateDisplaySettings(
                    userId,
                    backgroundColor,
                    globeGlowColor,
                    globePointColor,
                    globeMarkerColor);
        }
        return new DisplaySettingsDTO(
                backgroundColor,
                globeGlowColor,
                globePointColor,
                globeMarkerColor);
    }

    private void validatePage(Integer defaultPage) {
        if (defaultPage == null || defaultPage < DEFAULT_PAGE || defaultPage > MAX_PAGE) {
            throw new BaseException(
                    HttpStatus.BAD_REQUEST.value(),
                    "defaultPage must be between 0 and 5");
        }
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new BaseException(
                    HttpStatus.BAD_REQUEST.value(),
                    "default currency values must be 3-letter currency codes");
        }
        return normalized;
    }

    private String normalizeColor(String color) {
        String normalized = color == null ? "" : color.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("#[0-9A-F]{6}")) {
            throw new BaseException(
                    HttpStatus.BAD_REQUEST.value(),
                    "display colors must use #RRGGBB format");
        }
        return normalized;
    }

    private DisplaySettingsDTO defaultDisplaySettings() {
        return new DisplaySettingsDTO(
                DEFAULT_BACKGROUND_COLOR,
                DEFAULT_GLOBE_GLOW_COLOR,
                DEFAULT_GLOBE_POINT_COLOR,
                DEFAULT_GLOBE_MARKER_COLOR);
    }
}
