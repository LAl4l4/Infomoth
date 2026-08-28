package ReleaseBack.Back.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ReleaseBack.Back.entity.MarketCorrelation;

@Mapper
public interface MarketCorrelationMapper {

    List<MarketCorrelation> findAll();

    int updateCorrelation(@Param("correlation") MarketCorrelation correlation);

    int insertCorrelation(@Param("correlation") MarketCorrelation correlation);
}
