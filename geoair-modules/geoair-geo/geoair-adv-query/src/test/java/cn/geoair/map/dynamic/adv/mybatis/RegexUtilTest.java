package cn.geoair.map.dynamic.adv.mybatis;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.adv.mybatis.util.RegexUtil;
import org.junit.Test;

/** {@link RegexUtil} 的单元测试。 */
public class RegexUtilTest {

    @Test
    public void testReplace_simpleItem() {
        String result = RegexUtil.replace("item.name", "item", "list[0]");
        assertEquals("list[0].name", result);
    }

    @Test
    public void testReplace_indexVariable() {
        String result = RegexUtil.replace("index", "index", "__index_ids[0]");
        assertEquals("__index_ids[0]", result);
    }

    @Test
    public void testReplace_noMatch() {
        // "otherItem" should NOT be replaced (negative lookahead)
        String result = RegexUtil.replace("otherItem.name", "item", "list[0]");
        assertEquals("otherItem.name", result);
    }

    @Test
    public void testReplace_itemInMiddle() {
        // Regex is anchored at ^, so item in middle should not match
        String result = RegexUtil.replace("prefix item.name", "item", "list[0]");
        assertEquals("prefix item.name", result);
    }

    @Test
    public void testReplace_withLeadingWhitespace() {
        String result = RegexUtil.replace("  item.name", "item", "list[0]");
        assertEquals("  list[0].name", result);
    }

    @Test
    public void testReplace_itemAsWholeWord() {
        // "item" alone (no suffix) should match
        String result = RegexUtil.replace("item", "item", "list[0]");
        assertEquals("list[0]", result);
    }

    @Test
    public void testReplace_itemWithBracket() {
        // "item[0]" — item is followed by "[" which is a separator, should match
        String result = RegexUtil.replace("item[0]", "item", "list[0]");
        assertEquals("list[0][0]", result);
    }
}
