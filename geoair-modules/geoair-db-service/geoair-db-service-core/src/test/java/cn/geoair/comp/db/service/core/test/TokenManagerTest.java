package cn.geoair.comp.db.service.core.test;

import static org.junit.Assert.*;

import cn.geoair.comp.db.service.core.utils.TokenManager;
import org.junit.Test;

/** {@link TokenManager} 的单元测试。 */
public class TokenManagerTest {

    @Test
    public void testGenerateAndValidate() {
        String token = TokenManager.generateToken("admin", "123456");
        assertNotNull(token);
        assertEquals(32, token.length());

        assertTrue(TokenManager.validateToken(token, "admin", "123456"));
    }

    @Test
    public void testValidateWrongPassword() {
        String token = TokenManager.generateToken("admin", "123456");
        assertFalse(TokenManager.validateToken(token, "admin", "654321"));
    }

    @Test
    public void testValidateWrongUsername() {
        String token = TokenManager.generateToken("admin", "123456");
        assertFalse(TokenManager.validateToken(token, "user", "123456"));
    }

    @Test
    public void testValidateNullToken() {
        assertFalse(TokenManager.validateToken(null, "admin", "123456"));
    }

    @Test
    public void testValidateEmptyToken() {
        assertFalse(TokenManager.validateToken("", "admin", "123456"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateWithNullUsername() {
        TokenManager.generateToken(null, "123456");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateWithNullPassword() {
        TokenManager.generateToken("admin", null);
    }
}
