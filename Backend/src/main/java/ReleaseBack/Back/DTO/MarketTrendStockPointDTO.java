package ReleaseBack.Back.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketTrendStockPointDTO {
    private String symbol;
    private String name;
    private Double price;
}
