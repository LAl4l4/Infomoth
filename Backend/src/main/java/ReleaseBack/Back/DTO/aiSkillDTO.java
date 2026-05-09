package ReleaseBack.Back.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class aiSkillDTO {
    private Integer rank;
    private String skill;
    private Integer mentions;
    private String date;
    private String source;

    @JsonProperty("sample_post_title")
    private String samplePostTitle;
}
