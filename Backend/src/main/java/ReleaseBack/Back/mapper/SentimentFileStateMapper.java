package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ReleaseBack.Back.entity.SentimentFileState;

@Mapper
public interface SentimentFileStateMapper {
    SentimentFileState find();

    int insert();

    int updatePoliticsSha256(@Param("sha256") String sha256);

    int updateTechSha256(@Param("sha256") String sha256);
}
