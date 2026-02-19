package ReleaseBack.Back.DTO;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import ReleaseBack.Back.entity.Profile;
import lombok.Data;

@Data
public class ProfileDTO {
    @JsonProperty("userid")
    private Integer id;

    private String bio;
    private LocalDate birthday;
    private Profile.Gender gender;
}
