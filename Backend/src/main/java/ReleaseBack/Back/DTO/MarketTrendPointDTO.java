package ReleaseBack.Back.DTO;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketTrendPointDTO {
    private LocalDate date;
    private Double sentiment;
    private List<MarketTrendStockPointDTO> stocks;
}
