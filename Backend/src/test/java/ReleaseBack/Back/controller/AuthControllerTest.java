package ReleaseBack.Back.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ReleaseBack.Back.DTO.ProfileDTO;
import ReleaseBack.Back.VO.ProfileVO;
import ReleaseBack.Back.VO.TokenVO;
import ReleaseBack.Back.entity.Profile;
import ReleaseBack.Back.entity.User;
import ReleaseBack.Back.security.JwtUtil;
import ReleaseBack.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginShouldReturnTokenWhenCredentialsCorrect() {
        User user = new User();
        user.setId(7);
        user.setPassword("pw");
        when(userService.findByNameEmail("alice")).thenReturn(user);
        when(jwtUtil.generateToken(7)).thenReturn("jwt-token");

        TokenVO result = authController.login("alice", "pw");

        assertEquals("登录成功", result.getResult());
        assertEquals("jwt-token", result.getToken());
    }

    @Test
    void loginShouldRejectWrongPassword() {
        User user = new User();
        user.setId(7);
        user.setPassword("correct");
        when(userService.findByNameEmail("alice")).thenReturn(user);

        TokenVO result = authController.login("alice", "wrong");

        assertEquals("用户名/邮箱或密码错误", result.getResult());
        assertNull(result.getToken());
    }

    @Test
    void registerShouldReturnExistsWhenUserAlreadyFound() {
        when(userService.findByNameEmail("alice")).thenReturn(new User());

        String result = authController.register("alice", "pw", "alice@example.com");

        assertEquals("用户名已存在", result);
    }

    @Test
    void registerShouldCreateUserWhenNameAvailable() {
        when(userService.findByNameEmail("alice")).thenReturn(null);

        String result = authController.register("alice", "pw", "alice@example.com");

        assertEquals("注册成功", result);
        verify(userService).createUserWithProfile("alice", "pw", "alice@example.com");
    }

    @Test
    void registerShouldReturnServerErrorWhenCreateThrows() {
        when(userService.findByNameEmail("alice")).thenReturn(null);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(userService).createUserWithProfile(any(), any(), any());

        String result = authController.register("alice", "pw", "alice@example.com");

        assertEquals("服务器发生异常", result);
    }

    @Test
    void pullProfilesShouldMapProfileFields() {
        Profile profile = new Profile();
        profile.setBio("hello");
        profile.setAvatarUrl("avatar.png");
        profile.setBirthday(LocalDate.of(2000, 1, 1));
        profile.setGender(Profile.Gender.female);
        when(request.getAttribute("userId")).thenReturn(11);
        when(userService.findProfileById(11)).thenReturn(profile);

        ProfileVO result = authController.pullProfiles(request);

        assertNotNull(result);
        assertEquals("hello", result.getBio());
        assertEquals("avatar.png", result.getAvatarUrl());
        assertEquals(LocalDate.of(2000, 1, 1), result.getBirthday());
        assertEquals(Profile.Gender.female, result.getGender());
    }

    @Test
    void pullProfilesShouldReturnNullWhenProfileMissing() {
        when(request.getAttribute("userId")).thenReturn(11);
        when(userService.findProfileById(11)).thenReturn(null);

        ProfileVO result = authController.pullProfiles(request);

        assertNull(result);
    }

    @Test
    void pushProfileShouldCallServiceWithRequestUserId() {
        ProfileDTO dto = new ProfileDTO();
        dto.setBio("new bio");
        dto.setBirthday(LocalDate.of(1999, 5, 20));
        dto.setGender(Profile.Gender.male);
        when(request.getAttribute("userId")).thenReturn(21);

        String result = authController.pushProfile(dto, request);

        assertEquals("保存成功", result);
        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(userService).updateProfile(captor.capture());
        Profile profile = captor.getValue();
        assertEquals(21, profile.getId());
        assertEquals("new bio", profile.getBio());
        assertEquals(LocalDate.of(1999, 5, 20), profile.getBirthday());
        assertEquals(Profile.Gender.male, profile.getGender());
    }

    @Test
    void pushProfileShouldReturnServerErrorWhenUpdateThrows() {
        ProfileDTO dto = new ProfileDTO();
        when(request.getAttribute("userId")).thenReturn(21);
        org.mockito.Mockito.doThrow(new RuntimeException("update fail"))
                .when(userService).updateProfile(any(Profile.class));

        String result = authController.pushProfile(dto, request);

        assertEquals("服务器发生异常", result);
    }
}
