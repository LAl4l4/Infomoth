package ReleaseBack.Back.security;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import java.time.Duration;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {
    private final String SECRET = "my_secret_key_which_should_be_long_enough";
    private final long EXPIRE = Duration.ofHours(48).toMillis();


    public String generateToken(Integer userId) {
        return Jwts.builder()
                .subject("auth")
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(getSigningKey())
                .compact();
    }

    public Integer parseId(String token) {
        return (Integer) Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId");
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }
}
