package ReleaseBack.Back.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;


@Data
@Entity
@Table(name = "profiles")
public class Profile {

    //mybatis会直接无视@Column等注解，那是JPA的，需要在xml里面自己配置映射
    private Integer id;

    private String bio;
    
    private String avatarUrl;

    private LocalDate birthday;

    private Gender gender;

    public enum Gender {
        male,
        female,
        other,
        undisclosed
    }
}
