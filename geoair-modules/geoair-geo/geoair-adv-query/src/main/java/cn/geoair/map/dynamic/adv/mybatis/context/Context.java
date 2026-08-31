package cn.geoair.map.dynamic.adv.mybatis.context;

import cn.geoair.map.dynamic.adv.mybatis.util.OgnlUtil;
import java.util.*;

/**
 * 动态 SQL 生成的运行时上下文，贯穿整个 SQL 解析过程。
 *
 * <p>职责：
 *
 * <ul>
 *   <li><b>SQL 拼接</b> — 通过 {@link #appendSql(String)} 逐步构建最终 SQL
 *   <li><b>参数收集</b> — 通过 {@link #addParameter(Object)} 收集 {@code #{}} 对应的 JDBC 参数值
 *   <li><b>表达式求值</b> — 通过 {@link #getOgnlValue(String)} 和 {@link #getOgnlBooleanValue(String)} 对
 *       OGNL 表达式求值
 * </ul>
 *
 * @author zhangjun
 */
public class Context {

    private StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> jdbcParameters = new ArrayList<>();
    private final Map<String, Object> data;

    /**
     * 创建上下文。
     *
     * @param data OGNL 求值的数据上下文（变量名 → 值的映射）
     */
    public Context(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * 追加 SQL 片段到 SQL 构建器。
     *
     * @param text SQL 文本，null 时忽略
     */
    public void appendSql(String text) {
        if (text != null) {
            sqlBuilder.append(text);
        }
    }

    /**
     * 添加一个 JDBC 参数值（对应一个 {@code ?} 占位符）。
     *
     * @param o 参数值
     */
    public void addParameter(Object o) {
        jdbcParameters.add(o);
    }

    /**
     * 通过 OGNL 表达式获取值。
     *
     * @param expression OGNL 表达式
     * @return 求值结果
     */
    public Object getOgnlValue(String expression) {
        return OgnlUtil.getValue(expression, data);
    }

    /**
     * 通过 OGNL 表达式获取布尔值。
     *
     * @param expression OGNL 表达式
     * @return 布尔结果
     */
    public Boolean getOgnlBooleanValue(String expression) {
        return OgnlUtil.getBooleanValue(expression, data);
    }

    /**
     * 获取当前拼接的 SQL 文本。
     *
     * @return SQL 字符串
     */
    public String getSql() {
        return sqlBuilder.toString();
    }

    /**
     * 替换当前 SQL 文本（用于 {@code #{}} → {@code ?} 的批量替换）。
     *
     * @param text 新的 SQL 文本
     */
    public void setSql(String text) {
        sqlBuilder = new StringBuilder(text);
    }

    /**
     * 获取已收集的 JDBC 参数列表（不可变视图）。
     *
     * @return 参数列表
     */
    public List<Object> getJdbcParameters() {
        return Collections.unmodifiableList(jdbcParameters);
    }

    /**
     * 获取 OGNL 求值的数据上下文。
     *
     * @return 数据映射
     */
    public Map<String, Object> getData() {
        return data;
    }
}
