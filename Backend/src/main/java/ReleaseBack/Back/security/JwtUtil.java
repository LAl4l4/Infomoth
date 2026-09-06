package ReleaseBack.Back.security;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import java.time.Duration;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {
    private final SecretKey signingKey;

    public JwtUtil(@Value("${JWT_SECRET:}") String secret,
            @Value("${AUTH_REQUIRE_SECRET:false}") boolean requireSecret) {
        if (requireSecret && secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured for deployment");
        }
        signingKey = secret.isBlank() ? Jwts.SIG.HS256.key().build()
                : Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
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
        return signingKey;
    }
}
