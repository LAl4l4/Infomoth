package ReleaseBack.Back.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import ReleaseBack.Back.entity.Profile;
import ReleaseBack.Back.entity.User;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=never"
})
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        // Crucial: H2 take user as reserved keyword, must set NON_KEYWORDS to avoid syntax error
        jdbcTemplate.execute("SET NON_KEYWORDS USER;");
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS user (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) NOT NULL,
                    password VARCHAR(100) NOT NULL,
                    email VARCHAR(200) NOT NULL
                )
                """
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS profiles (
                    user_id INT PRIMARY KEY,
                    bio VARCHAR(255),
                    avatar_url VARCHAR(255),
                    birthday DATE,
                    gender VARCHAR(30)
                )
                """
        );
        jdbcTemplate.execute("DELETE FROM profiles");
        jdbcTemplate.execute("DELETE FROM user");
    }

    @Test
    void findByUsernameAndFindByEmailShouldReturnMatchedUser() {
        jdbcTemplate.update(
                "INSERT INTO user(username, password, email) VALUES (?, ?, ?)",
                "alice", "pw1", "alice@example.com"
        );

        User byUsername = userMapper.findByUsername("alice");
        User byEmail = userMapper.findByEmail("alice@example.com");

        assertNotNull(byUsername);
        assertEquals("alice@example.com", byUsername.getEmail());
        assertNotNull(byEmail);
        assertEquals("alice", byEmail.getUsername());
        assertNull(userMapper.findByUsername("missing-user"));
    }

    @Test
    void saveUserShouldPersistAndBackfillGeneratedId() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword("pw2");
        user.setEmail("bob@example.com");

        userMapper.saveUser(user);

        assertNotNull(user.getId());
        User loaded = userMapper.findByUsername("bob");
        assertNotNull(loaded);
        assertEquals(user.getId(), loaded.getId());
        assertEquals("bob@example.com", loaded.getEmail());
        userMapper.updatePassword(user.getId(), "encoded-password");
        assertEquals("encoded-password", userMapper.findByUsername("bob").getPassword());
    }

    @Test
    void createProfileAndUpdatesShouldPersistAllFields() {
        User user = new User();
        user.setUsername("carol");
        user.setPassword("pw3");
        user.setEmail("carol@example.com");
        userMapper.saveUser(user);

        Profile profile = new Profile();
        profile.setId(user.getId());
        profile.setBio("initial bio");
        profile.setAvatarUrl("https://img.test/avatar.png");
        profile.setBirthday(LocalDate.of(1998, 1, 15));
        profile.setGender(Profile.Gender.undisclosed);
        userMapper.createProfile(profile);

        userMapper.updateProfileBio(user.getId(), "updated bio");
        userMapper.updateProfileBirthday(user.getId(), LocalDate.of(2000, 2, 20));
        userMapper.updateProfileGender(user.getId(), Profile.Gender.female);

        Profile loaded = userMapper.findProfileById(user.getId());
        assertNotNull(loaded);
        assertEquals(user.getId(), loaded.getId());
        assertEquals("updated bio", loaded.getBio());
        assertEquals("https://img.test/avatar.png", loaded.getAvatarUrl());
        assertEquals(LocalDate.of(2000, 2, 20), loaded.getBirthday());
        assertEquals(Profile.Gender.female, loaded.getGender());
    }
}
