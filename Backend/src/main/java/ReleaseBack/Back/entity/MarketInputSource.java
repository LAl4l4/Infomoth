package ReleaseBack.Back.entity;

import lombok.Data;

@Data
public class MarketInputSource {
    private String source;
    private String status;
    private String message;
    private Long attemptedAt;
}
