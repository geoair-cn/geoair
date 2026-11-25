package cn.geoair.comp.dynamic.ds.readwrite.proxy.druid;

import java.util.Calendar;

/**
 * JDBC 参数接口（参考 Druid）
 *
 * @author 张俊
 * @date 2026/5/28
 */
public interface JdbcParameter {

    public static final int BinaryInputStream = 10001;
    public static final int AsciiInputStream = 10002;
    public static final int CharacterInputStream = 10003;
    public static final int NCharacterInputStream = 10004;
    public static final int URL = 10005;

    public static interface TYPE {
        public static final int BinaryInputStream = 10001;
        public static final int AsciiInputStream = 10002;
        public static final int CharacterInputStream = 10003;
        public static final int NCharacterInputStream = 10004;
        public static final int URL = 10005;
        public static final int UnicodeStream = 10006;
        public static final int BYTES = 10007;
    }
    /** 获取参数值 */
    Object getValue();

    /** 获取长度（用于流类型） */
    long getLength();

    /** 获取日历（用于日期类型） */
    Calendar getCalendar();

    /** 获取 SQL 类型 */
    int getSqlType();
}
