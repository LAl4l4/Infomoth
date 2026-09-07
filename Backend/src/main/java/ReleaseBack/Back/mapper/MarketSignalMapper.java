package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketSignalMapper {
    String lockInputHash();
    String findSignature();
    String findPayload();
    int updateInputHash(@Param("hash") String hash);
    int updateSnapshot(@Param("signature") String signature, @Param("payload") String payload);
}
