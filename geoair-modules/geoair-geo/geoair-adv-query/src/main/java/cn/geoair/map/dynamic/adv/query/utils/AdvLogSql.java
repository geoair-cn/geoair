package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author 张俊
 * @date 2026/4/24
 * @description SQL 日志（智能换行，不截断单词 + 动态长度分割线）
 */
@Slf4j
public class AdvLogSql {

    // 全局开关
    public static boolean logEnable = true;

    // 一行最大长度（分割线自动根据这个生成）
    private static final int MAX_LINE_LENGTH = 180;

    // 动态生成的分割线（只生成一次）
    private static final String SPLIT_LINE;

    static {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_LINE_LENGTH; i++) {
            sb.append("=");
        }
        SPLIT_LINE = sb.toString();
    }

    // ===================== 颜色 =====================
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String BOLD = "\u001B[1;37m";
    private static final String GRAY = "\u001B[90m";
    private static final String BLUE = "\u001B[34m";

    private final IDataSourceGetter dataSourceGetter;

    public static AdvLogSql of(IDataSourceGetter dataSourceGetter) {
        return new AdvLogSql(dataSourceGetter);
    }

    private AdvLogSql(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    private String wrapSql(String sql) {
        if (sql == null || sql.isEmpty()) return "";
        if (sql.length() <= MAX_LINE_LENGTH) return sql;

        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        String[] tokens = sql.split("(?<=\\s|,|\\()|(?=\\s|,|\\))");

        for (String token : tokens) {
            if (currentLine.length() + token.length() > MAX_LINE_LENGTH && currentLine.length() > 0) {
                result.append(currentLine).append("\n");
                currentLine = new StringBuilder();
            }
            currentLine.append(token);
        }

        if (currentLine.length() > 0) {
            result.append(currentLine);
        }

        return result.toString().replace("\n ", "\n").trim();
    }

    // ===================== 带Class =====================
    public void logExecuteSql(Class callerClass, String methodName, String sql, long lastTaskTimeMillis) {
        if (!logEnable) return;
        log.debug("\n" + GRAY + SPLIT_LINE + RESET
                        + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + " | 耗时：" + YELLOW + "{}ms" + RESET
                        + "\n执行方法：{}.{}"
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n" + GRAY + SPLIT_LINE + RESET,
                getDatabaseName(), getSchemaName(), lastTaskTimeMillis,
                callerClass.getSimpleName(), methodName, wrapSql(sql));
    }

    // ===================== 带Class + 影响行数 =====================
    public void logExecuteSql(Class callerClass, String methodName, String sql, long lastTaskTimeMillis, Number rows) {
        if (!logEnable) return;
        log.debug("\n" + GRAY + SPLIT_LINE + RESET
                        + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + " | 耗时：" + YELLOW + "{}ms" + RESET + " | 影响行数：" + BLUE + "{}" + RESET
                        + "\n执行方法：{}.{}"
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n" + GRAY + SPLIT_LINE + RESET,
                getDatabaseName(), getSchemaName(), lastTaskTimeMillis, rows,
                callerClass.getSimpleName(), methodName, wrapSql(sql));
    }

    // ===================== 带Class + 参数 + 影响行数 =====================
    public void logExecuteSql(Class callerClass, String methodName, String sql, List<Object> params, long lastTaskTimeMillis, Number rows) {
        if (!logEnable) return;
        log.debug("\n" + GRAY + SPLIT_LINE + RESET
                        + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + " | 耗时：" + YELLOW + "{}ms" + RESET + " | 影响行数：" + BLUE + "{}" + RESET
                        + "\n执行方法：{}.{}"
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n参数列表：" + GREEN + "{}" + RESET
                        + "\n" + GRAY + SPLIT_LINE + RESET,
                getDatabaseName(), getSchemaName(), lastTaskTimeMillis, rows,
                callerClass.getSimpleName(), methodName, wrapSql(sql), params);
    }

    // ===================== 工具方法 =====================
    protected String getSchemaName() {
        if (dataSourceGetter == null) return "";
        String schema = dataSourceGetter.getSchemaName();
        return GutilObject.isEmpty(schema) ? "" : schema;
    }

    protected String getDatabaseName() {
        if (dataSourceGetter == null) return "";
        String db = dataSourceGetter.getDatabaseName();
        return GutilObject.isEmpty(db) ? "" : db;
    }
}
