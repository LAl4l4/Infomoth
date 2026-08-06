package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

import ReleaseBack.Back.entity.SentimentAverage;

@Mapper
public interface SentimentMapper {

    SentimentAverage findLatestByTable(@Param("table") String table);

    int insertPoliticsAverage(@Param("date") LocalDate date, @Param("sentimentScore") double sentimentScore);

    int updatePoliticsAverage(@Param("date") LocalDate date, @Param("sentimentScore") double sentimentScore);

    int insertTechAverage(@Param("date") LocalDate date, @Param("sentimentScore") double sentimentScore);

    int updateTechAverage(@Param("date") LocalDate date, @Param("sentimentScore") double sentimentScore);
}
