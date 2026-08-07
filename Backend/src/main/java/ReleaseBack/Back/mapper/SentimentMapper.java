package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

import ReleaseBack.Back.entity.SentimentAverage;

@Mapper
public interface SentimentMapper {

    SentimentAverage findLatestByTable(@Param("table") String table);

    List<SentimentAverage> findSince(
            @Param("table") String table,
            @Param("fromDate") LocalDate fromDate);

    int insertPoliticsAverage(@Param("date") LocalDate date, @Param("sentimentScore") double sentimentScore);

    int updatePoliticsAverage(@Param("date") LocalDate date, @Param("sentimentScore") double sentimentScore);

    int insertTechAverage(@Param("date") LocalDate date, @Param("sentimentScore") double sentimentScore);

    int updateTechAverage(@Param("date") LocalDate date, @Param("sentimentScore") double sentimentScore);
}
