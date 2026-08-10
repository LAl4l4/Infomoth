package ReleaseBack.Back.entity;

import java.time.LocalDate;

import lombok.Data;

@Data
public class UsStockIndexRecord {
    private Integer id;
    private LocalDate date;
    private Double sp500Price;
    private Double sp500ChangePercent;
    private Double dowJonesPrice;
    private Double dowJonesChangePercent;
    private Double nasdaqPrice;
    private Double nasdaqChangePercent;
    private Double russell2000Price;
    private Double russell2000ChangePercent;
}
