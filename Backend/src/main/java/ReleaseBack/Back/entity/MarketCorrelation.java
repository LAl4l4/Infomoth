package ReleaseBack.Back.entity;

import lombok.Data;

@Data
public class MarketCorrelation {

    private String symbol;
    private String name;
    private Double corr;
    private Integer sampleSize;
}
