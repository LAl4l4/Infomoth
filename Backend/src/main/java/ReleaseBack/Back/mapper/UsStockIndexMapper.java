package ReleaseBack.Back.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ReleaseBack.Back.entity.UsStockIndexRecord;

@Mapper
public interface UsStockIndexMapper {
    List<UsStockIndexRecord> findSince(@Param("fromDate") LocalDate fromDate);

    int updateDailyChange(@Param("snapshot") UsStockIndexRecord snapshot);

    int insertDailyChange(@Param("snapshot") UsStockIndexRecord snapshot);
}
