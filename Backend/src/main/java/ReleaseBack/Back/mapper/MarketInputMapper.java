package ReleaseBack.Back.mapper;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import ReleaseBack.Back.entity.MarketInputSource;
import ReleaseBack.Back.entity.MarketObservation;

@Mapper
public interface MarketInputMapper {
    List<MarketObservation> findRecent(@Param("indicator") String indicator,
        @Param("date") LocalDate date, @Param("today") LocalDate today, @Param("now") long now);
    List<MarketObservation> findSince(@Param("date") LocalDate date);
    MarketObservation findObservation(@Param("indicator") String indicator, @Param("date") LocalDate date);
    int insertObservation(MarketObservation observation);
    int updateObservation(MarketObservation observation);
    List<MarketInputSource> findSources();
    int insertSource(MarketInputSource source);
    int updateSource(MarketInputSource source);
}
