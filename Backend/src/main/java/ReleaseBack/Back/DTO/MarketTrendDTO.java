package ReleaseBack.Back.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketTrendDTO {
    private List<MarketTrendPointDTO> points;
    private List<MarketCorrelationDTO> correlations;
}
