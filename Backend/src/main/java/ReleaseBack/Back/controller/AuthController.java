package ReleaseBack.Back.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import ReleaseBack.Back.entity.User;
import ReleaseBack.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ReleaseBack.Back.entity.Profile;
import ReleaseBack.Back.DTO.ProfileDTO;
import ReleaseBack.Back.VO.*;
import ReleaseBack.Back.security.JwtUtil;
import io.jsonwebtoken.JwtException;

import java.time.Duration;


@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "authToken";
    private static final Duration AUTH_COOKIE_MAX_AGE = Duration.ofHours(48);

    private final UserService userService;
    
    private final JwtUtil jwtUtil;

    public record Credentials(String username, String pass, String email) {}

    private TokenVO authResult(boolean success, String message) {
        TokenVO result = new TokenVO();
        result.setSuccess(success);
        result.setResult(message);
        return result;
    }

    @PostMapping("/login")
    public TokenVO login(
        @RequestBody Credentials credentials,
        HttpServletResponse response
    ) {
        User foundUser = userService.authenticate(credentials.username(), credentials.pass());
        TokenVO tokenVO = new TokenVO();
        if (foundUser != null) {
            String token = jwtUtil.generateToken(foundUser.getId());
            response.addHeader(HttpHeaders.SET_COOKIE, buildAuthCookie(token, AUTH_COOKIE_MAX_AGE).toString());
            tokenVO.setSuccess(true);
            tokenVO.setResult("登录成功");
            return tokenVO;
        }
        tokenVO.setResult("用户名/邮箱或密码错误");
        return tokenVO;
    }

    @GetMapping("/session")
    public TokenVO session(@CookieValue(name = AUTH_COOKIE_NAME, required = false) String token) {
        TokenVO response = new TokenVO();
        if (token != null && !token.isBlank()) {
            try {
                jwtUtil.parseId(token);
                response.setSuccess(true);
                response.setResult("登录有效");
                return response;
            } catch (JwtException | IllegalArgumentException ignored) {
                // Treat an invalid or expired cookie as an unauthenticated session.
            }
        }
        response.setResult("未登录");
        return response;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildAuthCookie("", Duration.ZERO).toString());
    }


    @PostMapping("/register")
    public TokenVO register(@RequestBody Credentials credentials) {
        String username = credentials.username();
        String pass = credentials.pass();
        String email = credentials.email();
        if (username == null || username.isBlank() || username.contains("@")
                || pass == null || pass.isBlank() || pass.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 || email == null || email.isBlank()) {
            return authResult(false, "请完整填写有效的注册信息");
        }
        User foundUser = userService.findByNameEmail(username);

        if (foundUser != null) {
            return authResult(false, "用户名已存在");
        }

        try {
            userService.createUserWithProfile(username, pass, email);
        } catch (Exception e) {
            e.printStackTrace();
            return authResult(false, "服务器发生异常");
        }
        
        return authResult(true, "注册成功");
    }

    
    
    @GetMapping("/pullProfiles")
    public ProfileVO pullProfiles(HttpServletRequest request) {
        int userid = (int) request.getAttribute("userId");
        Profile profile = userService.findProfileById(userid);
        if (profile == null) {
            return null;
        }
        ProfileVO profileVO = new ProfileVO();
        profileVO.setBio(profile.getBio());
        profileVO.setAvatarUrl(profile.getAvatarUrl());
        profileVO.setBirthday(profile.getBirthday());
        profileVO.setGender(profile.getGender());
        return profileVO;
    }

    @PostMapping("/pushProfile")
    public String pushProfile(@RequestBody ProfileDTO profileDTO, HttpServletRequest request) {
        try {
            int userid = (int) request.getAttribute("userId");
            Profile profile = new Profile();
            profile.setId(userid);
            profile.setBio(profileDTO.getBio());
            profile.setBirthday(profileDTO.getBirthday());
            profile.setGender(profileDTO.getGender());
            userService.updateProfile(profile);
        } catch (Exception e) {
            e.printStackTrace();
            return "服务器发生异常";
        }
        return "保存成功";
    }

    private ResponseCookie buildAuthCookie(String token, @NonNull Duration maxAge) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
    
}
