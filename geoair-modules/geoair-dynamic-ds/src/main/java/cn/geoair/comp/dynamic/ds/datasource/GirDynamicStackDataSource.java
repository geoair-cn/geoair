package cn.geoair.comp.dynamic.ds.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.Map;
import java.util.Stack;

/**
 * @author ：张俊
 * @date ：Created in 2024/12/31 15:43
 * @description： 用于替换spring托管的数据源，支持嵌套方法调用的数据源切换
 */
@Slf4j
public class GirDynamicStackDataSource extends AbstractRoutingDataSource {

    /**
     * 线程局部变量 用来保存数据源名称栈
     */
    private static final ThreadLocal<Stack<String>> contextHolder =
            ThreadLocal.withInitial(Stack::new);


    @Override
    protected Object determineCurrentLookupKey() {
        return getCurrentDataSource();
    }

    /**
     * 将接收到的主库从库数据源和默认数据源（主库）配置写入AbstractRoutingDataSource类的targetDataSources这个Map
     *
     * @param targetDataSources       目标数据源映射，键为数据源名称，值为数据源实例
     * @param defaultTargetDataSource 默认数据源实例
     */
    public GirDynamicStackDataSource(
            Map<Object, Object> targetDataSources, Object defaultTargetDataSource) {
        // 打印所有配置的数据源键
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder("配置的数据源列表：\n");
            if (targetDataSources != null) {
                for (Object key : targetDataSources.keySet()) {
                    sb.append("  - 数据源键: ")
                            .append(key)
                            .append(" (")
                            .append(targetDataSources.get(key).getClass().getSimpleName())
                            .append(")\n");
                }
            }
            log.info(sb.toString());

            // 打印默认数据源信息
            if (defaultTargetDataSource != null) {
                log.info("默认数据源类型: {}", defaultTargetDataSource.getClass().getSimpleName());
            }
        }

        // 设置目标数据源和默认数据源
        if (targetDataSources != null) {
            super.setTargetDataSources(targetDataSources);
        }
        if (defaultTargetDataSource != null) {
            super.setDefaultTargetDataSource(defaultTargetDataSource);
        }
        super.afterPropertiesSet(); // 确保数据源初始化
    }

    /**
     * 将数据源名称压入栈顶，从当前数据源切换到新的数据源
     *
     * <p>如果栈为空，视为从默认数据源切换
     *
     * @param dataSource 要切换到的新数据源名称
     */
    public static void pushDataSource(String dataSource) {
        Stack<String> stack = contextHolder.get();
        String previousDataSource = stack.isEmpty() ? "默认数据源" : stack.peek();
        stack.push(dataSource);
        log.debug("数据源已从 [{}] 切换到 [{}]", previousDataSource, dataSource);
    }


    /**
     * 从栈顶弹出当前数据源，恢复到之前的数据源上下文
     *
     * <p>如果栈中只有一个元素，弹出后将清除所有数据源上下文， 下次获取数据源时将使用默认数据源。
     */
    public static void popDataSource() {
        Stack<String> stack = contextHolder.get();
        if (stack.isEmpty()) {
            log.warn("尝试弹出数据源时，数据源栈已为空！这可能表示数据源上下文管理不正确。");
            return;
        }

        String currentDataSource = stack.pop();
        String previousDataSource = stack.isEmpty() ? "默认数据源" : stack.peek();

        // log.info("数据源已从 [{}] 恢复到 [{}]",
        // currentDataSource == null ? "默认数据源" : currentDataSource,
        // previousDataSource);

        // 如果栈为空，清除线程局部变量
        if (stack.isEmpty()) {
            contextHolder.remove();
            log.trace("数据源栈已清空，线程局部变量已移除");
        }
    }

    /**
     * 获取当前线程的数据源栈
     *
     * @return 数据源名称栈，如果未设置则返回空栈
     */
    public static Stack<String> getDataSourceStack() {
        Stack<String> stack = contextHolder.get();

        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder("获取当前数据源栈: [");
            if (!stack.isEmpty()) {
                for (int i = 0; i < stack.size(); i++) {
                    sb.append(stack.get(i));
                    if (i < stack.size() - 1) {
                        sb.append(" -> ");
                    }
                }
            } else {
                sb.append("空栈");
            }
            sb.append("]");
            log.trace(sb.toString());
        }

        return stack;
    }

    /**
     * 获取当前使用的数据源
     *
     * @return 当前数据源名称，如果未设置则返回null（表示使用默认数据源）
     */
    public static String getCurrentDataSource() {
        Stack<String> stack = contextHolder.get();
        String current = stack.isEmpty() ? null : stack.peek();

        // log.info("当前使用的数据源: {}", current == null ? "默认数据源" : current);
        return current;
    }

    /**
     * 完全清空当前线程的数据源栈，恢复到默认数据源
     *
     * <p>在线程池环境中，建议在每个任务执行前后调用此方法，防止上下文污染
     */
    public static void clearAllDataSources() {
        Stack<String> stack = contextHolder.get();
        if (!stack.isEmpty()) {
            log.warn("检测到线程残留的数据源上下文，正在清理: {}", String.join(" -> ", stack));
            stack.clear();
        }
        contextHolder.remove();
        log.debug("已完全清空数据源栈，恢复到默认数据源");
    }

    /**
     * @return 可能为null或不正确的数据源名称
     * @deprecated 此方法已过时，请使用 {@link #getCurrentDataSource()} 替代。
     * <p>原因：原方法使用简单的ThreadLocal存储数据源名称，在嵌套方法调用中会导致上下文丢失。 新方法使用栈结构管理数据源，支持多层嵌套调用，并确保正确恢复数据源上下文。
     */
    @Deprecated
    public static String getDataSource() {
        log.warn(
                "警告: getDataSource() 方法已过时，请使用 getCurrentDataSource() 替代。"
                        + "原方法不支持嵌套数据源切换，可能导致数据访问异常。");
        String dataSourceName = contextHolder.get().isEmpty() ? null : contextHolder.get().peek();
        log.info("(已过时) 当前数据源: {}", dataSourceName);
        return dataSourceName;
    }

    /**
     * @param dataSource 数据源名称
     * @deprecated 此方法已过时，请使用 {@link #pushDataSource(String)} 和 {@link #popDataSource()} 替代。
     * <p>原因：原方法使用简单的ThreadLocal.set()，在嵌套方法调用中会覆盖上层方法的数据源设置，
     * 且无法正确恢复上下文。新方法使用栈结构管理数据源，确保多层嵌套调用的正确性。
     */
    @Deprecated
    public static void setDataSource(String dataSource) {
        log.warn(
                "警告: setDataSource() 方法已过时，请使用 pushDataSource() 和 popDataSource() 替代。"
                        + "原方法不支持嵌套数据源切换，可能导致数据访问异常。");
        // 兼容处理：如果栈为空，先push一个null作为占位符
        Stack<String> stack = contextHolder.get();
        if (stack.isEmpty()) {
            stack.push(null);
        }
        // 替换栈顶元素
        stack.pop();
        stack.push(dataSource);
        log.info("(已过时) 设置数据源: {}", dataSource);
    }

    /**
     * @deprecated 此方法已过时，请使用 {@link #popDataSource()} 替代。
     * <p>原因：原方法使用简单的ThreadLocal.remove()，在嵌套方法调用中会完全清除上下文，
     * 导致上层方法无法恢复其数据源设置。新方法使用栈结构管理数据源，确保正确恢复上层上下文。
     */
    @Deprecated
    public static void clearDataSource() {
        log.warn(
                "警告: clearDataSource() 方法已过时，请使用 popDataSource() 替代。"
                        + "原方法不支持嵌套数据源切换，可能导致数据访问异常。");
        Stack<String> stack = contextHolder.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        // 如果栈为空，清除线程局部变量
        if (stack.isEmpty()) {
            contextHolder.remove();
        }
        log.info("(已过时) 清除数据源上下文");
    }
}
