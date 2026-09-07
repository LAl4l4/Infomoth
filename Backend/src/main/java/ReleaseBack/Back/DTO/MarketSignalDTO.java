package ReleaseBack.Back.DTO;

import java.time.LocalDate;
import java.util.List;
import ReleaseBack.Back.entity.MarketInputSource;

public record MarketSignalDTO(Prediction prediction, List<MarketInputSource> sources) {
    public record Prediction(String modelVersion, String target, String horizon, long generatedAt,
                             Double score, double coverage, String status, List<Input> inputs) {}
    public record Input(String indicator, String label, String unit, String source, String sourceUrl,
                        LocalDate date, Double value, Long fetchedAt, Long availableAt, String status,
                        String transform, Double normalized, double weight, double effectiveWeight,
                        Double contribution, List<Point> history) {}
    public record Point(LocalDate date, double value) {}
}
