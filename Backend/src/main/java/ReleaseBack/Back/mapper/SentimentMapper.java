package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import ReleaseBack.Back.entity.SentimentAverage;

@Mapper
public interface SentimentMapper {

    SentimentAverage findLatestByTable(@Param("table") String table);
}
