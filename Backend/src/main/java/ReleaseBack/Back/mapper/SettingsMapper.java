package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettingsMapper {
    Integer findDefaultPageByUserId(@Param("userId") Integer userId);

    String findDefaultBaseCurrencyByUserId(@Param("userId") Integer userId);

    String findDefaultQuoteCurrencyByUserId(@Param("userId") Integer userId);

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
}
