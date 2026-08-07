package ReleaseBack.Back.entity;

import java.time.LocalDate;

import lombok.Data;

@Data
public class UsStockIndexRecord {
    private Integer id;
    private String symbol;
    private String name;
    private Double price;
    private Double changeValue;
    private Double changePercent;
    private LocalDate date;
    private String source;
}
