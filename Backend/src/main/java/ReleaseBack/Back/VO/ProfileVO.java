package ReleaseBack.Back.VO;

import lombok.Data;
import java.time.LocalDate;
import ReleaseBack.Back.entity.Profile;

@Data
public class ProfileVO {
    
    private Integer id;
    private String bio;
    private String avatarUrl;
    private LocalDate birthday;
    private Profile.Gender gender;
}
