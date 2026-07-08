package ReleaseBack.Back.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class usStockIndexDTO {
    private String symbol;
    private String name;
    private Double price;
    private Double change;

    private Double changePercent;

    private String date;
    private String source;
}
