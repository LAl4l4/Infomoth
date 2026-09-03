package ReleaseBack.Back.entity;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SentimentAverage {

    private Integer id;
    private LocalDate date;
    private Double sentimentScore;
    private Double rollingAverage;
    private Double rollingStandardDeviation;
    private Integer sampleCount;
}
