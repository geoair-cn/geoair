package cn.geoair.map.dynamic.tools.page;

import cn.hutool.log.StaticLog;
import java.util.List;
import java.util.function.Consumer;

/**
 * 分页执行所需的查询、消费与错误处理回调。
 *
 * <p>页码从 0 还是 1 开始由 {@link PageConfig#isPageNumStartByZero()} 决定； {@link #getPageRecords(Integer,
 * Integer)} 必须按传入页码和页大小返回对应页数据。
 *
 * @param <T> 数据记录类型（泛型）
 */
public interface PageConditionDef<T> {

    /**
     * 获取总条数，用于计算分页信息
     *
     * @return 总记录数；返回 {@code null} 时由 {@link PageConfig#getTotalCount()} 兜底
     */
    Long getTotalRecordCount();

    /**
     * 设置分页配置
     *
     * @param pageConfig 分页配置
     */
    void setPageConfig(PageConfig pageConfig);

    /**
     * 获取单条记录的消费者
     *
     * @return 单条记录消费逻辑
     */
    default Consumer<T> getEachRecordConsumer() {
        return t -> {}; // 默认空消费
    }

    /**
     * 分页查询异常处理器
     *
     * @param pageNo 发生异常的页码
     * @param pageSize 每页条数
     * @param e 异常对象
     * @return true-跳过当前页继续执行，false-终止整体执行
     */
    default boolean handlePageException(Integer pageNo, Integer pageSize, Exception e) {
        // 默认逻辑：打印异常日志，跳过当前页继续执行
        StaticLog.error(e, "分页查询异常：页码[{}]，页大小[{}]，异常信息：{}", pageNo, pageSize, e.getMessage());
        return true;
    }

    /**
     * 根据页码和页大小获取分页记录
     *
     * @param pageNo 页码（适配0/1起始）
     * @param pageSize 每页条数
     * @return 分页记录列表
     */
    List<T> getPageRecords(Integer pageNo, Integer pageSize);

    /**
     * 当分页任务正常完成或提前终止后执行的回调。
     *
     * @param resultList 仅在启用保存结果时包含收集的数据，否则为空列表
     * @param actualPageSize 实际采用的页大小
     * @param totalPages 实际执行的总页数
     * @param countConsume 已消费的记录数
     * @param totalCount 查询前得到的总记录数
     */
    default void onComplete(
            List<T> resultList,
            long actualPageSize,
            long totalPages,
            long countConsume,
            long totalCount) {
        StaticLog.info(
                "查询结束，结果列表大小：{},计算后的分页大小 {}，总页数:{} ,消费总条数：{},分页总条数：{}",
                resultList.size(),
                actualPageSize,
                totalPages,
                countConsume,
                totalCount);
    }
}
