package cn.geoair.map.dynamic.adv.mybatis;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** {@link DynamicSqlEngine} 的单元测试。 */
public class DynamicSqlEngineTest {

    private final DynamicSqlEngine engine = new DynamicSqlEngine();

    @Test
    public void testParse_simpleIf() {
        String sql = "<if test='minId != null'>id > #{minId}</if>";
        Map<String, Object> params = new HashMap<>();
        params.put("minId", 100);

        SqlMeta result = engine.parse(sql, params);
        assertEquals("id > ?", result.getSql());
        assertEquals(1, result.getJdbcParamValues().size());
        assertEquals(100, result.getJdbcParamValues().get(0));
    }

    @Test
    public void testParse_nestedIf() {
        String sql =
                "<if test='minId != null'>id > #{minId} <if test='maxId != null'> and id &lt; #{maxId}</if></if>";
        Map<String, Object> params = new HashMap<>();
        params.put("minId", 100);
        params.put("maxId", 500);

        SqlMeta result = engine.parse(sql, params);
        assertEquals("id > ?  and id < ?", result.getSql());
        assertEquals(2, result.getJdbcParamValues().size());
        assertEquals(100, result.getJdbcParamValues().get(0));
        assertEquals(500, result.getJdbcParamValues().get(1));
    }

    @Test
    public void testParse_ifFalse() {
        String sql = "<if test='minId != null'>id > #{minId}</if>";
        Map<String, Object> params = new HashMap<>();
        // minId not set → condition is false

        SqlMeta result = engine.parse(sql, params);
        assertEquals("", result.getSql().trim());
        assertTrue(result.getJdbcParamValues().isEmpty());
    }

    @Test
    public void testParse_dollarSubstitution() {
        String sql = "SELECT ${columns} FROM user WHERE id = #{id}";
        Map<String, Object> params = new HashMap<>();
        params.put("columns", "id, name");
        params.put("id", 42);

        SqlMeta result = engine.parse(sql, params);
        assertEquals("SELECT id, name FROM user WHERE id = ?", result.getSql());
        assertEquals(1, result.getJdbcParamValues().size());
        assertEquals(42, result.getJdbcParamValues().get(0));
    }

    @Test
    public void testParse_foreach() {
        String sql =
                "SELECT * FROM user WHERE id IN <foreach collection='ids' open='(' close=')' separator=',' item='item'>#{item}</foreach>";
        Map<String, Object> params = new HashMap<>();
        params.put("ids", new int[] {1, 2, 3});

        SqlMeta result = engine.parse(sql, params);
        assertTrue(result.getSql().contains("(?,?,?)"));
        assertEquals(3, result.getJdbcParamValues().size());
    }

    @Test
    public void testParse_whereTrim() {
        String sql =
                "<where><if test='name != null'>AND name = #{name}</if><if test='age != null'>AND age = #{age}</if></where>";
        Map<String, Object> params = new HashMap<>();
        params.put("name", "test");
        // age not set

        SqlMeta result = engine.parse(sql, params);
        assertTrue(result.getSql().trim().startsWith("WHERE"));
        assertTrue(result.getSql().contains("name = ?"));
        assertFalse(result.getSql().contains("age"));
    }

    @Test
    public void testParse_setTrim() {
        String sql =
                "UPDATE user <set><if test='name != null'>name = #{name},</if><if test='age != null'>age = #{age},</if></set> WHERE id = #{id}";
        Map<String, Object> params = new HashMap<>();
        params.put("name", "test");
        params.put("id", 1);

        SqlMeta result = engine.parse(sql, params);
        assertTrue(result.getSql().trim().contains("SET"));
        assertTrue(result.getSql().contains("name = ?"));
        assertFalse(result.getSql().contains("age"));
    }

    @Test
    public void testParse_toString() {
        String sql = "SELECT * FROM user WHERE id = #{id}";
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);

        SqlMeta result = engine.parse(sql, params);
        String str = result.toString();
        assertTrue(str.contains("SELECT"));
        assertTrue(str.contains("1"));
    }

    @Test
    public void testExtractParameterNames() {
        String sql =
                "<if test='minId != null'>id > #{minId} <if test='maxId != null'> and id &lt; #{maxId}</if></if>";
        Set<String> params = engine.extractParameterNames(sql);
        assertTrue(params.contains("minId"));
        assertTrue(params.contains("maxId"));
    }

    @Test
    public void testExtractParameterNames_dollarAndHash() {
        String sql = "SELECT ${columns} FROM user WHERE id = #{id}";
        Set<String> params = engine.extractParameterNames(sql);
        assertTrue(params.contains("columns"));
        assertTrue(params.contains("id"));
    }

    @Test
    public void testCache_reuse() {
        String sql = "SELECT * FROM user WHERE id = #{id}";
        Map<String, Object> p1 = new HashMap<>();
        p1.put("id", 1);
        Map<String, Object> p2 = new HashMap<>();
        p2.put("id", 2);

        SqlMeta r1 = engine.parse(sql, p1);
        SqlMeta r2 = engine.parse(sql, p2);
        // Same template → both should parse correctly (cache hit)
        assertEquals("SELECT * FROM user WHERE id = ?", r1.getSql());
        assertEquals("SELECT * FROM user WHERE id = ?", r2.getSql());
        assertEquals(1, r1.getJdbcParamValues().get(0));
        assertEquals(2, r2.getJdbcParamValues().get(0));
    }
}
