package ReleaseBack.Back.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import ReleaseBack.Back.entity.User;
import ReleaseBack.Back.entity.Profile;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);

    User findByEmail(@Param("email") String email);

    void saveUser(User user);

    void updatePassword(@Param("userid") Integer userid, @Param("password") String password);

    void createProfile(Profile profile);

    Profile findProfileById(@Param("userid") Integer userid);

    void updateProfileBio(
        @Param("userid") Integer userid, 
        @Param("bio") String bio);

    void updateProfileBirthday(
        @Param("userid") Integer userid, 
        @Param("birthday") java.time.LocalDate birthday);

    void updateProfileGender(
        @Param("userid") Integer userid, 
        @Param("gender") Profile.Gender gender);
}
