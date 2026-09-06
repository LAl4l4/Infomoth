package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ReleaseBack.Back.entity.Profile;
import ReleaseBack.Back.entity.User;
import ReleaseBack.Back.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void findByNameEmailShouldUseEmailLookupWhenInputContainsAt() {
        User expected = new User();
        expected.setUsername("alice");
        when(userMapper.findByEmail("alice@example.com")).thenReturn(expected);

        User result = userService.findByNameEmail("alice@example.com");

        assertEquals(expected, result);
        verify(userMapper).findByEmail("alice@example.com");
        verify(userMapper, never()).findByUsername(any());
    }

    @Test
    void findByNameEmailShouldUseUsernameLookupWhenNoAt() {
        User expected = new User();
        expected.setUsername("alice");
        when(userMapper.findByUsername("alice")).thenReturn(expected);

        User result = userService.findByNameEmail("alice");

        assertEquals(expected, result);
        verify(userMapper).findByUsername("alice");
        verify(userMapper, never()).findByEmail(any());
    }

    @Test
    void createUserWithProfileShouldCreateBothUserAndDefaultProfile() {
        org.mockito.Mockito.doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(123);
            return null;
        }).when(userMapper).saveUser(any(User.class));

        userService.createUserWithProfile("new-user", "pw", "new@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).saveUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("new-user", savedUser.getUsername());
        org.junit.jupiter.api.Assertions.assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("pw", savedUser.getPassword()));
        assertEquals("new@example.com", savedUser.getEmail());

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(userMapper).createProfile(profileCaptor.capture());
        Profile createdProfile = profileCaptor.getValue();
        assertEquals(123, createdProfile.getId());
        assertEquals("This user is too lazy to leave anything here.", createdProfile.getBio());
        assertEquals(Profile.Gender.undisclosed, createdProfile.getGender());
        assertEquals(null, createdProfile.getAvatarUrl());
        assertEquals(null, createdProfile.getBirthday());
    }

    @Test
    void updateProfileShouldOnlyUpdateProvidedFields() {
        Profile profile = new Profile();
        profile.setId(11);
        profile.setBio("new bio");
        profile.setBirthday(LocalDate.of(2001, 3, 9));
        profile.setGender(Profile.Gender.other);

        userService.updateProfile(profile);

        verify(userMapper).updateProfileBio(11, "new bio");
        verify(userMapper).updateProfileBirthday(11, LocalDate.of(2001, 3, 9));
        verify(userMapper).updateProfileGender(11, Profile.Gender.other);
    }

    @Test
    void updateProfileShouldSkipNullFields() {
        Profile profile = new Profile();
        profile.setId(11);
        profile.setBio(null);
        profile.setBirthday(null);
        profile.setGender(Profile.Gender.male);

        userService.updateProfile(profile);

        verify(userMapper, never()).updateProfileBio(any(), any());
        verify(userMapper, never()).updateProfileBirthday(any(), any());
        verify(userMapper).updateProfileGender(11, Profile.Gender.male);
    }

    @Test
    void saveAndFindMethodsShouldDelegateToMapper() {
        User user = new User();
        user.setId(1);
        Profile profile = new Profile();
        profile.setId(1);
        when(userMapper.findProfileById(1)).thenReturn(profile);

        userService.saveUser(user);
        userService.createProfile(profile);
        Profile loaded = userService.findProfileById(1);

        verify(userMapper).saveUser(user);
        verify(userMapper).createProfile(profile);
        assertNotNull(loaded);
        assertEquals(1, loaded.getId());
    }

    @Test
    void legacyPasswordIsUpgradedOnlyAfterSuccessfulLogin() {
        User user = new User();
        user.setId(3);
        user.setPassword("legacy-password");
        when(userMapper.findByUsername("legacy")).thenReturn(user);
        org.junit.jupiter.api.Assertions.assertNull(userService.authenticate("legacy", "wrong"));
        verify(userMapper, never()).updatePassword(any(), any());
        assertEquals(user, userService.authenticate("legacy", "legacy-password"));
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updatePassword(org.mockito.ArgumentMatchers.eq(3), hash.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("legacy-password", hash.getValue()));
        user.setPassword(hash.getValue());
        assertEquals(user, userService.authenticate("legacy", "legacy-password"));
        org.junit.jupiter.api.Assertions.assertNull(userService.authenticate("legacy", "wrong"));
        verify(userMapper, org.mockito.Mockito.times(1)).updatePassword(any(), any());
    }

    @Test
    void invalidLoginDoesNotQueryDatabase() {
        org.junit.jupiter.api.Assertions.assertNull(userService.authenticate(null, "pw"));
        org.junit.jupiter.api.Assertions.assertNull(userService.authenticate("alice", null));
        org.mockito.Mockito.verifyNoInteractions(userMapper);
    }
}
