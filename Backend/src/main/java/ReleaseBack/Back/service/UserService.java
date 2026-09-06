package ReleaseBack.Back.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ReleaseBack.Back.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import ReleaseBack.Back.entity.User;
import ReleaseBack.Back.entity.Profile;

@RequiredArgsConstructor
@Service
public class UserService {
    
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwords =
            new BCryptPasswordEncoder(12);

    @Transactional
    public User authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null
                || password.getBytes(StandardCharsets.UTF_8).length > 72) return null;
        User user = findByNameEmail(username);
        if (user == null || user.getPassword() == null) return null;
        String stored = user.getPassword();
        if (stored.startsWith("$2")) {
            return passwords.matches(password, stored) ? user : null;
        }
        // Upgrade legacy plaintext only after successful verification.
        if (!MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8))) return null;
        userMapper.updatePassword(user.getId(), passwords.encode(password));
        return user;
    }


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
        newUser.setPassword(passwords.encode(password));
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
