package ReleaseBack.Back.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ReleaseBack.Back.mapper.UserMapper;
import ReleaseBack.Back.entity.User;
import ReleaseBack.Back.entity.Profile;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;

    public User findByNameEmail(String username) {
        if (username.contains("@")){
            return userMapper.findByEmail(username);
        }else{
            return userMapper.findByUsername(username);
        }
    }

    @Transactional
    public void createUserWithProfile(
        String username,
        String password,
        String email
    ) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(email);

        userMapper.saveUser(newUser);

        // 2️⃣ 创建 profile，并手动同步 id
        Profile newProfile = new Profile();
        newProfile.setId(newUser.getId()); // ⭐ 这一步 = @MapsId
        newProfile.setBio("This user is too lazy to leave anything here.");
        newProfile.setAvatarUrl(null);
        newProfile.setBirthday(null);
        newProfile.setGender(Profile.Gender.undisclosed);

        userMapper.createProfile(newProfile);
    }


    public void saveUser(User user) {
        userMapper.saveUser(user);
    }

    public void createProfile(Profile profile) {
        userMapper.createProfile(profile);
    }

    public Profile findProfileById(Integer userid) {
        return userMapper.findProfileById(userid);
    }

    @Transactional
    public void updateProfile(Profile profile) {
        if (profile.getBio() != null)
        userMapper.updateProfileBio(profile.getId(), profile.getBio());

        if (profile.getBirthday() != null)
        userMapper.updateProfileBirthday(profile.getId(), profile.getBirthday());

        if (profile.getGender() != null)
        userMapper.updateProfileGender(profile.getId(), profile.getGender());
    }

}
