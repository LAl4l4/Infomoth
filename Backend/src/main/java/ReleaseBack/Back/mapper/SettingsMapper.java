package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettingsMapper {
    Integer findDefaultPageByUserId(@Param("userId") Integer userId);

    void insertDefaultPage(
            @Param("userId") Integer userId,
            @Param("defaultPage") Integer defaultPage);

    void updateDefaultPage(
            @Param("userId") Integer userId,
            @Param("defaultPage") Integer defaultPage);
}
