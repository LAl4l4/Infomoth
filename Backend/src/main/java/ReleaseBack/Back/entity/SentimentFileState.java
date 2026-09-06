package ReleaseBack.Back.entity;

import lombok.Data;

/** The single-row checkpoint for the two source sentiment files. */
@Data
public class SentimentFileState {
    private Integer id;
    private String politicsSha256;
    private String techSha256;
}
