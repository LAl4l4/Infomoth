package ReleaseBack.Back.DTO;

import lombok.Data;

@Data
public class exchangeRateDTO {
    private String base;
    private String quote;
    private Double rate; // 这里的类型会自动转换
    private String date;
    private String source;
}
