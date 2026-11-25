package cn.geoair.map.dynamic.adv.mybatis;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.adv.mybatis.token.TokenParser;
import org.junit.Test;

/** {@link TokenParser} 的单元测试。 */
public class TokenParserTest {

    @Test
    public void testParse_singleToken() {
        TokenParser parser = new TokenParser("#{", "}", content -> "?" + content);
        String result = parser.parse("id = #{minId}");
        assertEquals("id = ?minId", result);
    }

    @Test
    public void testParse_multipleTokens() {
        TokenParser parser = new TokenParser("#{", "}", content -> "?");
        String result = parser.parse("id > #{minId} and id < #{maxId}");
        assertEquals("id > ? and id < ?", result);
    }

    @Test
    public void testParse_noToken() {
        TokenParser parser = new TokenParser("#{", "}", content -> "?");
        String result = parser.parse("SELECT * FROM user");
        assertEquals("SELECT * FROM user", result);
    }

    @Test
    public void testParse_escapedToken() {
        TokenParser parser = new TokenParser("#{", "}", content -> "?");
        String result = parser.parse("id = \\#{notAToken} and id = #{real}");
        assertEquals("id = #{notAToken} and id = ?", result);
    }

    @Test
    public void testParse_escapedCloseToken() {
        TokenParser parser = new TokenParser("#{", "}", content -> "?" + content);
        String result = parser.parse("id = #{expr\\}}");
        assertEquals("id = ?expr}", result);
    }

    @Test
    public void testParse_emptyText() {
        TokenParser parser = new TokenParser("#{", "}", content -> "?");
        assertEquals("", parser.parse(""));
        assertEquals("", parser.parse(null));
    }

    @Test
    public void testParse_unclosedToken() {
        TokenParser parser = new TokenParser("#{", "}", content -> "?");
        String result = parser.parse("id = #{unclosed");
        assertEquals("id = #{unclosed", result);
    }

    @Test
    public void testParse_dollarToken() {
        TokenParser parser = new TokenParser("${", "}", content -> "RESOLVED");
        String result = parser.parse("SELECT ${columns} FROM user");
        assertEquals("SELECT RESOLVED FROM user", result);
    }

    @Test
    public void testParse_tokenWithWhitespace() {
        TokenParser parser = new TokenParser("#{", "}", content -> "?" + content);
        String result = parser.parse("id = #{ minId }");
        assertEquals("id = ?minId", result);
    }
}
