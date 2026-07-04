package cn.ginna.marxexercise.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 */
class JwtUtilTest {

    private static final String SECRET = "test-jwt-secret-key-for-unit-tests-only-min-32-chars";
    private static final long EXPIRATION = 86400000L;

    private JwtUtil createJwtUtil() {
        return new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    void testGenerateAndParseToken() {
        JwtUtil jwtUtil = createJwtUtil();
        String token = jwtUtil.generateToken(1L, "testUser", "USER");

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
        assertEquals(1L, jwtUtil.getUserIdFromToken(token));
        assertEquals("testUser", jwtUtil.getUsernameFromToken(token));
        assertEquals("USER", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void testInvalidTokenThrowsException() {
        JwtUtil jwtUtil = createJwtUtil();
        assertThrows(Exception.class, () -> jwtUtil.getUserIdFromToken("invalid.token.here"));
    }

    @Test
    void testDifferentUsersGenerateDifferentTokens() {
        JwtUtil jwtUtil = createJwtUtil();
        String token1 = jwtUtil.generateToken(1L, "alice", "USER");
        String token2 = jwtUtil.generateToken(2L, "bob", "ADMIN");
        assertNotEquals(token1, token2);
    }

    @Test
    void testAdminRoleInToken() {
        JwtUtil jwtUtil = createJwtUtil();
        String token = jwtUtil.generateToken(42L, "adminUser", "ADMIN");
        assertEquals("ADMIN", jwtUtil.getRoleFromToken(token));
        assertEquals(42L, jwtUtil.getUserIdFromToken(token));
    }
}
