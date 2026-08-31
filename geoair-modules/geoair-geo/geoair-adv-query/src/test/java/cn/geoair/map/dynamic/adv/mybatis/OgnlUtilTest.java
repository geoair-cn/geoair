package cn.geoair.map.dynamic.adv.mybatis;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.adv.mybatis.util.OgnlUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** {@link OgnlUtil} 的单元测试。 */
public class OgnlUtilTest {

    @Test
    public void testGetValue_simpleProperty() {
        Map<String, Object> root = new HashMap<>();
        root.put("name", "hello");
        assertEquals("hello", OgnlUtil.getValue("name", root));
    }

    @Test
    public void testGetValue_listIndex() {
        Map<String, Object> root = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        list.add(10);
        list.add(20);
        list.add(30);
        root.put("ids", list);
        assertEquals(20, OgnlUtil.getValue("ids[1]", root));
    }

    @Test
    public void testGetValue_nullProperty() {
        Map<String, Object> root = new HashMap<>();
        assertNull(OgnlUtil.getValue("missing", root));
    }

    @Test
    public void testGetBooleanValue_true() {
        Map<String, Object> root = new HashMap<>();
        root.put("flag", true);
        assertTrue(OgnlUtil.getBooleanValue("flag", root));
    }

    @Test
    public void testGetBooleanValue_false() {
        Map<String, Object> root = new HashMap<>();
        root.put("flag", false);
        assertFalse(OgnlUtil.getBooleanValue("flag", root));
    }

    @Test
    public void testGetBooleanValue_numberNonZero() {
        Map<String, Object> root = new HashMap<>();
        root.put("count", 5);
        assertTrue(OgnlUtil.getBooleanValue("count", root));
    }

    @Test
    public void testGetBooleanValue_numberZero() {
        Map<String, Object> root = new HashMap<>();
        root.put("count", 0);
        assertFalse(OgnlUtil.getBooleanValue("count", root));
    }

    @Test(expected = RuntimeException.class)
    public void testGetBooleanValue_invalidType() {
        Map<String, Object> root = new HashMap<>();
        root.put("name", "notABoolean");
        OgnlUtil.getBooleanValue("name", root);
    }

    @Test
    public void testGetIterable_list() {
        Map<String, Object> root = new HashMap<>();
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        root.put("items", list);

        Iterable<?> result = OgnlUtil.getIterable("items", root);
        int count = 0;
        for (Object o : result) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testGetIterable_array() {
        Map<String, Object> root = new HashMap<>();
        root.put("arr", new int[] {1, 2, 3});

        Iterable<?> result = OgnlUtil.getIterable("arr", root);
        int count = 0;
        for (Object o : result) {
            count++;
        }
        assertEquals(3, count);
    }

    @Test(expected = RuntimeException.class)
    public void testGetIterable_null() {
        Map<String, Object> root = new HashMap<>();
        OgnlUtil.getIterable("missing", root);
    }

    @Test
    public void testGetValue_expression() {
        Map<String, Object> root = new HashMap<>();
        root.put("a", 10);
        root.put("b", 20);
        Object result = OgnlUtil.getValue("a + b", root);
        assertEquals(30, ((Number) result).intValue());
    }
}
