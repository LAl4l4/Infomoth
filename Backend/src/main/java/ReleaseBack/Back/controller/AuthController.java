package ReleaseBack.Back.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ReleaseBack.Back.entity.User;
import ReleaseBack.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import ReleaseBack.Back.entity.Profile;
import ReleaseBack.Back.DTO.ProfileDTO;
import ReleaseBack.Back.VO.*;
import ReleaseBack.Back.security.JwtUtil;


@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000") 
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    //测试账号 admin@admin.com/admin1/admin1

    @PostMapping("/login")
    public TokenVO login(
        @RequestParam String username, //可能是email或username
        @RequestParam String pass
    ) {
        User foundUser = userService.findByNameEmail(username);
        TokenVO tokenVO = new TokenVO();
        if (foundUser != null && foundUser.getPassword().equals(pass)) {
            String token = jwtUtil.generateToken(foundUser.getId());
            tokenVO.setResult("登录成功");
            tokenVO.setToken(token);
            return tokenVO;
        }
        tokenVO.setResult("用户名/邮箱或密码错误");
        tokenVO.setToken(null);
        return tokenVO;
    }


    @PostMapping("/register")
    @Transactional
    public String register(
        @RequestParam String username, 
        @RequestParam String pass,
        @RequestParam String email
    ) {
        User foundUser = userService.findByNameEmail(username);

        if (foundUser != null) {
            return "用户名已存在";
        }

        try {
            userService.createUserWithProfile(username, pass, email);
        } catch (Exception e) {
            return "服务器发生异常";
        }
        
        return "注册成功";
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
            return "服务器发生异常";
        }
        return "保存成功";
    }
}
