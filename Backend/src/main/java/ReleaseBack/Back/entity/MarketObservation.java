package ReleaseBack.Back.entity;

import java.time.LocalDate;
import lombok.Data;

@Data
public class MarketObservation {
    private String indicator;
    private LocalDate date;
    private Double value;
    private String source;
    private Long fetchedAt;
    private Long availableAt;
}
