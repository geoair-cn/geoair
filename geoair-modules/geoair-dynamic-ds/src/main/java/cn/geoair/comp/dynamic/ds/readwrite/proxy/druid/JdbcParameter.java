package cn.geoair.comp.dynamic.ds.readwrite.proxy.druid;

import java.util.Calendar;

/**
 * JDBC 参数接口（参考 Druid）
 *
 * @author 张俊
 * @date 2026/5/28
 */
public interface JdbcParameter {

    // 特殊类型常量
    int BINARY_STREAM = 10001;
    int ASCII_STREAM = 10002;
    int CHARACTER_STREAM = 10003;
    int NCHARACTER_STREAM = 10004;
    int URL = 10005;
    int UNICODE_STREAM = 10006;
    int BYTES = 10007;

    /**
     * 获取参数值
     */
    Object getValue();

    /**
     * 获取长度（用于流类型）
     */
    long getLength();

    /**
     * 获取日历（用于日期类型）
     */
    Calendar getCalendar();

    /**
     * 获取 SQL 类型
     */
    int getSqlType();
}
