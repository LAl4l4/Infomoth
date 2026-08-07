package ReleaseBack.Back.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketCorrelationDTO {
    private String symbol;
    private String name;
    private Double corr;
    private Integer sampleSize;
}
