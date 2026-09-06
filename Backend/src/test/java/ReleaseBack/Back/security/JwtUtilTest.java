package ReleaseBack.Back.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    @Test
    void configuredKeySurvivesRestartAndRotationRejectsOldTokens() {
        String secret = "a".repeat(64);
        String token = new JwtUtil(secret, true).generateToken(7);
        assertEquals(7, new JwtUtil(secret, true).parseId(token));
        assertThrows(io.jsonwebtoken.JwtException.class,
                () -> new JwtUtil("b".repeat(64), true).parseId(token));
    }

    @Test
    void noConfiguredKeyUsesIndependentRandomKeysAndRejectsWeakKeys() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("", true));
        String token = new JwtUtil("", false).generateToken(7);
        assertThrows(io.jsonwebtoken.JwtException.class, () -> new JwtUtil("", false).parseId(token));
        assertThrows(io.jsonwebtoken.security.WeakKeyException.class, () -> new JwtUtil("short", true));
    }
}
