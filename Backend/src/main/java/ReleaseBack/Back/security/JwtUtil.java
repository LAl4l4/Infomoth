package ReleaseBack.Back.security;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

@Component
public class JwtUtil {
    private final String SECRET = "my_secret_key";
    private final long EXPIRE = 1000 * 60 * 15; // 15分钟


    public String generateToken(Integer userId) {
        return Jwts.builder()
                .setSubject("auth")
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    public Integer parseId(String token) {
        return (Integer) Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody()
                .get("userId");
    }
}
