package cn.geoair.map.dynamic.adv.mybatis.context;

import cn.geoair.map.dynamic.adv.mybatis.util.OgnlUtil;
import java.util.*;

/**
 * 动态 SQL 生成的运行时上下文，贯穿整个 SQL 解析过程。
 * <p>
 * 职责：
 * <ul>
 *   <li><b>SQL 拼接</b> — 通过 {@link #appendSql(String)} 逐步构建最终 SQL</li>
 *   <li><b>参数收集</b> — 通过 {@link #addParameter(Object)} 收集 {@code #{}} 对应的 JDBC 参数值</li>
 *   <li><b>表达式求值</b> — 通过 {@link #getOgnlValue(String)} 和 {@link #getOgnlBooleanValue(String)} 对 OGNL 表达式求值</li>
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
     * @param data OGNL 求值的数据上下文（变量名 → 值的映射）。构造时会进行浅拷贝，
     *             因此解析过程写入的内部变量不会回写到调用方。
     */
    public Context(Map<String, Object> data) {
        this.data = data == null ? new HashMap<String, Object>() : new HashMap<>(data);
    }

    /**
     * 创建共享指定数据映射的内部上下文。
     *
     * <p>仅供动态 SQL 节点在同一次解析期间创建子上下文使用。这样嵌套
     * {@code <foreach>} 写入的内部索引变量可以被当前解析的根上下文继续使用，
     * 但根上下文本身仍然不会修改调用方传入的 Map。</p>
     *
     * @param data 当前解析持有的数据映射
     * @return 共享当前解析数据的子上下文
     */
    public static Context withSharedData(Map<String, Object> data) {
        return new Context(data, false);
    }

    private Context(Map<String, Object> data, boolean copyData) {
        if (data == null) {
            this.data = new HashMap<>();
        } else {
            this.data = copyData ? new HashMap<>(data) : data;
        }
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
