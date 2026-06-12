package cn.geoair.map.dynamic.tools.page;

import cn.hutool.log.StaticLog;
import java.util.List;
import java.util.function.Consumer;

/**
 * 分页定义
 *
 * @param <T> 数据记录类型（泛型）
 */
public interface PageConditionDef<T> {

    /**
     * 获取总条数，用于计算分页信息
     *
     * @param
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

    /** 当查询结束的时候。 */
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
