package cn.geoair.map.tile.forge.fuser.precache;

/**
 * @author ：zhangjun
 * @date ：Created in 2026/6/27 13:47
 * @description： 任务类型枚举
 */
public enum TaskType {
    PRE_CACHE("预缓存", "PreCache"),
    CHECK_REPAIR("检查修复", "Check"),
    ORIGINAL_CHECK_REPAIR("原始网格检查修复", "OriginalCheck"),
    ORIGINAL_PRE_CACHE("原始网格预缓存", "OriginalPreCache");

    private final String description;
    private final String prefix;

    TaskType(String description, String prefix) {
        this.description = description;
        this.prefix = prefix;
    }

    public String getDescription() {
        return description;
    }

    public String getPrefix() {
        return prefix;
    }
}
