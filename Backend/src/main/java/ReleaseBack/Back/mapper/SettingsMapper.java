package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ReleaseBack.Back.DTO.DisplaySettingsDTO;

@Mapper
public interface SettingsMapper {
    Integer findDefaultPageByUserId(@Param("userId") Integer userId);

    String findDefaultBaseCurrencyByUserId(@Param("userId") Integer userId);

    String findDefaultQuoteCurrencyByUserId(@Param("userId") Integer userId);

    DisplaySettingsDTO findDisplaySettingsByUserId(@Param("userId") Integer userId);

    void insertDefaultPage(
            @Param("userId") Integer userId,
            @Param("defaultPage") Integer defaultPage);

    void updateDefaultPage(
            @Param("userId") Integer userId,
            @Param("defaultPage") Integer defaultPage);

    void insertSettings(
            @Param("userId") Integer userId,
            @Param("defaultPage") Integer defaultPage,
            @Param("defaultBaseCurrency") String defaultBaseCurrency,
            @Param("defaultQuoteCurrency") String defaultQuoteCurrency);

    void updateDefaultBaseCurrency(
            @Param("userId") Integer userId,
            @Param("defaultBaseCurrency") String defaultBaseCurrency);

    void updateDefaultQuoteCurrency(
            @Param("userId") Integer userId,
            @Param("defaultQuoteCurrency") String defaultQuoteCurrency);

    void insertDisplaySettings(
            @Param("userId") Integer userId,
            @Param("backgroundColor") String backgroundColor,
            @Param("globeGlowColor") String globeGlowColor,
            @Param("globePointColor") String globePointColor,
            @Param("globeMarkerColor") String globeMarkerColor);

    void updateDisplaySettings(
            @Param("userId") Integer userId,
            @Param("backgroundColor") String backgroundColor,
            @Param("globeGlowColor") String globeGlowColor,
            @Param("globePointColor") String globePointColor,
            @Param("globeMarkerColor") String globeMarkerColor);
}
