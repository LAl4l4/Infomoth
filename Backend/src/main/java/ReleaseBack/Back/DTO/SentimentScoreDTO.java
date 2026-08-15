package ReleaseBack.Back.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentScoreDTO {
    private Double instant;
    private Double dailyAverage;
}
