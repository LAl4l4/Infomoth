package ReleaseBack.Back.DTO;

import java.time.LocalDate;

import ReleaseBack.Back.entity.Profile;
import lombok.Data;

@Data
public class ProfileDTO {

    private String bio;
    private LocalDate birthday;
    private Profile.Gender gender;
}
