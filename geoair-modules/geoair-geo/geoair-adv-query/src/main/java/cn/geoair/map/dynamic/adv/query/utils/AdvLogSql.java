package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.base.lang.caller.GkCallerUtil;
import cn.geoair.base.log.*;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.hutool.core.lang.caller.CallerUtil;
import cn.hutool.log.dialect.console.ConsoleColorLogFactory;
import cn.hutool.log.level.Level;

import java.util.List;

/**
 * @author 张俊
 * @date 2026/4/24
 * @description SQL 日志（智能换行，不截断单词 + 动态长度分割线）
 */

public class AdvLogSql extends GirLogWrapper {


    // 全局开关
    public static boolean logEnable = true;


    // 一行最大长度
    public static final int MAX_LINE_LENGTH = 180;

    // 动态生成的分割线
    private static final String SPLIT_LINE;

    static {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_LINE_LENGTH; i++) {
            sb.append("=");
        }
        SPLIT_LINE = sb.toString();
    }

    protected void recordLog(GemLogLevel level, String message, LoggerInfo loggerInfo, Object... arguments) {
        Class<?> caller = GkCallerUtil.getCaller(3);
        if (!getEnableByClassName(caller)) {
            return;
        }
        super.recordLog(level, message, loggerInfo, arguments);
    }


    protected void recordLogWithThrowable(GemLogLevel level, String message, Throwable t, LoggerInfo loggerInfo, Object... arguments) {
        Class<?> caller = GkCallerUtil.getCaller(3);
        if (!getEnableByClassName(caller)) {
            return;
        }
        super.recordLogWithThrowable(level, message, t, loggerInfo, arguments);
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

    private final AdvQueryGlobalConfig advQueryGlobalConfig;


    public static AdvLogSql of(IDataSourceGetter dataSourceGetter, AdvQueryGlobalConfig advQueryGlobalConfig) {
        return new AdvLogSql(dataSourceGetter, advQueryGlobalConfig);
    }


    private AdvLogSql(IDataSourceGetter dataSourceGetter, AdvQueryGlobalConfig advQueryGlobalConfig) {
        this.dataSourceGetter = dataSourceGetter;
        this.advQueryGlobalConfig = advQueryGlobalConfig;
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
        if (!getEnableByClassName(callerClass)) {
            return;
        }
        super.getTargetLoggerInfo(callerClass.getName()).getLogger().info("\n" + GRAY + SPLIT_LINE + RESET
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
        if (!getEnableByClassName(callerClass)) {
            return;
        }
        try {
            super.getTargetLoggerInfo(callerClass.getName()).getLogger().info("\n" + GRAY + SPLIT_LINE + RESET
                            + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + " | 耗时：" + YELLOW + "{}ms" + RESET + " | 影响行数：" + BLUE + "{}" + RESET
                            + "\n执行方法：{}.{}"
                            + "\nSQL 语句："
                            + "\n" + BOLD + "{}" + RESET
                            + "\n" + GRAY + SPLIT_LINE + RESET,
                    getDatabaseName(), getSchemaName(), lastTaskTimeMillis, rows,
                    callerClass.getSimpleName(), methodName, wrapSql(sql));
        } catch (Exception e) {

        }

    }

    // ===================== 带Class + 参数 + 影响行数 =====================
    public void logExecuteSql(Class callerClass, String methodName, String sql, List<Object> params, long lastTaskTimeMillis, Number rows) {
        if (!logEnable) return;
        if (!getEnableByClassName(callerClass)) {
            return;
        }
        super.getTargetLoggerInfo(callerClass.getName()).getLogger().info("\n" + GRAY + SPLIT_LINE + RESET
                        + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + " | 耗时：" + YELLOW + "{}ms" + RESET + " | 影响行数：" + BLUE + "{}" + RESET
                        + "\n执行方法：{}.{}"
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n参数列表：" + GREEN + "{}" + RESET
                        + "\n" + GRAY + SPLIT_LINE + RESET,
                getDatabaseName(), getSchemaName(), lastTaskTimeMillis, rows,
                callerClass.getSimpleName(), methodName, wrapSql(sql), params);
    }


    public void logExecuteError(Class callerClass, String methodName, String sql, Exception e) {
        if (!logEnable) return;
        if (!getEnableErrorLog(callerClass)) {
            return;
        }
        try {
            super.getTargetLoggerInfo(callerClass.getName()).getLogger().error("\n" + GRAY + SPLIT_LINE + RESET
                            + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET
                            + "\n执行方法：{}.{}"
                            + "\n【SQL 执行异常】"
                            + "\nSQL 语句："
                            + "\n" + BOLD + "{}" + RESET
                            + "\n异常信息：" + YELLOW + "{}" + RESET
                            + "\n" + GRAY + SPLIT_LINE + RESET,
                    getDatabaseName(), getSchemaName(),
                    callerClass.getSimpleName(), methodName,
                    wrapSql(sql),
                    e.getMessage());
        } catch (Exception e2) {
        }

    }

    // ===================== 异常日志：带耗时 =====================
    public void logExecuteError(Class callerClass, String methodName, String sql, long lastTaskTimeMillis, Exception e) {
        if (!logEnable) return;
        if (!getEnableErrorLog(callerClass)) {
            return;
        }
        super.getTargetLoggerInfo(callerClass.getName()).getLogger().error("\n" + GRAY + SPLIT_LINE + RESET
                        + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + " | 耗时：" + YELLOW + "{}ms" + RESET
                        + "\n执行方法：{}.{}"
                        + "\n【SQL 执行异常】"
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n异常信息：" + YELLOW + "{}" + RESET
                        + "\n" + GRAY + SPLIT_LINE + RESET,
                getDatabaseName(), getSchemaName(), lastTaskTimeMillis,
                callerClass.getSimpleName(), methodName,
                wrapSql(sql),
                e.getMessage());
    }

    // ===================== 异常日志：带参数 + 耗时 =====================
    public void logExecuteErrorSimple(Class callerClass, String methodName, String sql, List<Object> params, Exception e) {
        if (!logEnable) return;
        if (!getEnableErrorLog(callerClass)) {
            return;
        }
        super.getTargetLoggerInfo(callerClass.getName()).getLogger().error("\n" + GRAY + SPLIT_LINE + RESET
                        + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + RESET
                        + "\n执行方法：{}.{}"
                        + "\n【SQL 执行异常】"
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n参数列表：" + GREEN + "{}" + RESET
                        + "\n异常信息：" + YELLOW + "{}" + RESET
                        + "\n" + GRAY + SPLIT_LINE + RESET,
                getDatabaseName(), getSchemaName(),
                callerClass.getSimpleName(), methodName,
                wrapSql(sql),
                params,
                e.getMessage());
    }

    // ===================== 异常日志  =====================
    public void logExecuteErrorWithStack(Class callerClass, String methodName, String sql, List<Object> params, String lastTaskTimeMillis, Exception e) {
        if (!logEnable) return;
        if (!getEnableErrorLog(callerClass)) {
            return;
        }
        try {
            super.getTargetLoggerInfo(callerClass.getName()).getLogger().error("\n" + GRAY + SPLIT_LINE + RESET
                            + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + " | 耗时：" + YELLOW + "{}ms" + RESET
                            + "\n执行方法：{}.{}"
                            + "\n【SQL 执行异常 - 堆栈】"
                            + "\nSQL 语句："
                            + "\n" + BOLD + "{}" + RESET
                            + "\n参数列表：" + GREEN + "{}" + RESET
                            + "\n异常堆栈：",
                    getDatabaseName(), getSchemaName(), lastTaskTimeMillis,
                    callerClass.getSimpleName(), methodName,
                    wrapSql(sql),
                    params,
                    e);
        } catch (Exception ex) {

        }

    }

    public void logExecuteErrorWithStack(Class callerClass, String methodName, String sql, List<Object> params, Exception e) {
        logExecuteErrorWithStack(callerClass, methodName, sql, params, "不统计", e);
    }


    public void logExecuteError(Class<?> callerClass, String methodName, String sql, List<Object> params, String lastTaskTimeMillis, Exception e) {
        if (!logEnable) return;
        if (!getEnableErrorLog(callerClass)) {
            return;
        }
        super.getTargetLoggerInfo(callerClass.getName()).getLogger().error("\n" + GRAY + SPLIT_LINE + RESET
                        + "\n数据库 ：" + CYAN + "{}" + RESET + " | Schema ：" + CYAN + "{}" + RESET + " | 耗时：" + YELLOW + "{}ms" + RESET
                        + "\n执行方法：{}.{}"
                        + "\n【SQL 执行异常】"
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n拼接后可直接执行SQL："
                        + "\n" + GREEN + "{}" + RESET
                        + "\n参数列表：{}"
                        + "\n异常信息：" + YELLOW + "{}" + RESET
                        + "\n" + GRAY + SPLIT_LINE + RESET,
                getDatabaseName(), getSchemaName(), lastTaskTimeMillis,
                callerClass.getSimpleName(), methodName,
                wrapSql(sql),
                splicingSql(wrapSql(sql), params), // 自动拼接
                params,
                e.getMessage()
        );
    }

    public void logExecuteError(Class<?> callerClass, String methodName, String sql, List<Object> params, Exception e) {
        logExecuteError(callerClass, methodName, sql, params, "不统计", e);
    }

    /**
     * 把 ? 参数和 params 列表拼接成真实可执行 SQL（简单版）
     */
    private String splicingSql(String sql, List<Object> params) {
        if (sql == null || params == null || params.isEmpty()) {
            return sql;
        }

        try {
            int index = 0;
            StringBuilder realSql = new StringBuilder();
            String[] parts = sql.split("\\?");

            for (int i = 0; i < parts.length; i++) {
                realSql.append(parts[i]);
                if (index < params.size() && i < parts.length - 1) {
                    Object val = params.get(index++);
                    if (val == null) {
                        realSql.append("NULL");
                    } else if (val instanceof Number) {
                        realSql.append(val);
                    } else {
                        realSql.append("'").append(val.toString().replace("'", "''")).append("'");
                    }
                }
            }
            return realSql.toString();
        } catch (Exception e) {
            return "参数拼接失败：" + e.getMessage();
        }
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

    boolean getEnableByClassName(Class<?> callerClass) {
        String simpleName = callerClass.getSimpleName();
        if (simpleName.contains("DeleteOpt")) {
            return getConfig().isEnableDelLog();
        }
        if (simpleName.contains("UpdateOpt")) {
            return getConfig().isEnableUpdateLog();
        }
        if (simpleName.contains("SelectOpt")) {
            return getConfig().isEnableQueryLog();
        }
        if (simpleName.contains("AccessOpt")) {
            return getConfig().isEnableAccessLog();
        }

        if (simpleName.contains("DDLOpt")) {
            return getConfig().isEnableDdlLog();
        }
        return true;
    }

    boolean getEnableErrorLog(Class<?> callerClass) {
        return getConfig().isEnableErrorLog();
    }

    public AdvQueryGlobalConfig getConfig() {
        return advQueryGlobalConfig;
    }
}
