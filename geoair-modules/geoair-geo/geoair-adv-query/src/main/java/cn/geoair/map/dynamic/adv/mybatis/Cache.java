package cn.geoair.map.dynamic.adv.mybatis;

import cn.geoair.map.dynamic.adv.mybatis.node.SqlNode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQL 模板的 SqlNode 缓存，避免重复解析相同的 XML 模板。
 *
 * <p>使用 LRU 策略的有界缓存（默认最大 1024 条），当缓存满时淘汰最久未使用的条目。 缓存键为包含 {@code <root>} 包装的完整 XML 字符串。
 *
 * <p>线程安全说明：本类不是线程安全的。在多线程环境中，应由外部（如 {@link DynamicSqlEngine}） 保证同步访问。
 *
 * @author zhangjun
 */
public class Cache {

    private static final int MAX_SIZE = 1024;

    private final Map<String, SqlNode> nodeCache =
            new LinkedHashMap<String, SqlNode>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SqlNode> eldest) {
                    return size() > MAX_SIZE;
                }
            };

    /**
     * 从缓存中获取 SqlNode。
     *
     * @param key XML 模板字符串
     * @return 缓存的 SqlNode，不存在时返回 null
     */
    public SqlNode get(String key) {
        return nodeCache.get(key);
    }

    /**
     * 将 SqlNode 存入缓存。
     *
     * @param key XML 模板字符串
     * @param sqlNode 解析后的 SqlNode
     */
    public void put(String key, SqlNode sqlNode) {
        nodeCache.put(key, sqlNode);
    }
}
