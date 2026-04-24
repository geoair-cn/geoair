package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author 张俊
 * @date 2026/4/24
 * @description SQL执行日志（彩色高亮版 · JDK8兼容）
 */
@Slf4j
public class LogSqlUtils {

    // ===================== ANSI 控制台颜色 =====================
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";    // 青色（库名）
    private static final String YELLOW = "\u001B[33m";  // 黄色（耗时）
    private static final String GREEN = "\u001B[32m";   // 绿色（参数）
    private static final String BOLD = "\u001B[1;37m";  // 白色加粗（SQL）
    private static final String GRAY = "\u001B[90m";    // 灰色（分割线）

    private final IDataSourceGetter dataSourceGetter;

    public static LogSqlUtils of(IDataSourceGetter dataSourceGetter) {
        return new LogSqlUtils(dataSourceGetter);
    }

    private LogSqlUtils(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    // ===================== 日志输出 =====================
    public void logExecuteSql(String methodName, String sql, long lastTaskTimeMillis) {
        log.debug(GRAY + "==============================================" + RESET
                        + "\n执行方法：{}"
                        + "\n数据库 ：" + CYAN + "{}" + RESET
                        + "\nSchema ：" + CYAN + "{}" + RESET
                        + "\n耗时   ：" + YELLOW + "{}ms" + RESET
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n" + GRAY + "==============================================" + RESET,
                methodName,
                getDatabaseName(),
                getSchemaName(),
                lastTaskTimeMillis,
                sql);
    }

    public void logExecuteSql(String methodName, String sql, List<Object> params, long lastTaskTimeMillis) {
        log.debug(GRAY + "==============================================" + RESET
                        + "\n执行方法：{}"
                        + "\n数据库 ：" + CYAN + "{}" + RESET
                        + "\nSchema ：" + CYAN + "{}" + RESET
                        + "\n耗时   ：" + YELLOW + "{}ms" + RESET
                        + "\nSQL 语句："
                        + "\n" + BOLD + "{}" + RESET
                        + "\n参数列表：" + GREEN + "{}" + RESET
                        + "\n" + GRAY + "==============================================" + RESET,
                methodName,
                getDatabaseName(),
                getSchemaName(),
                lastTaskTimeMillis,
                sql,
                params);
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
