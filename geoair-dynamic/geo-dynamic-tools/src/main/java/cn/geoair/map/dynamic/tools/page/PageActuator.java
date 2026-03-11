package cn.geoair.map.dynamic.tools.page;

import cn.geoair.base.Gir;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.LongStream;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/18 13:34 @description： 分页执行器
 */
public class PageActuator<T> {

    private final PageConditionDef<T> pageConditionDef;

    /** 分页配置 */
    PageConfig pageConfig = new PageConfig();

    private final List<T> finalDataList = new ArrayList<T>();

    // 仅用于标记是否终止
    private final AtomicBoolean isTerminate = new AtomicBoolean(false);

    // 私有化构造器，通过静态方法创建实例
    private PageActuator(PageConditionDef<T> pageConditionDef) {
        this.pageConditionDef = pageConditionDef;
    }

    public List<T> getFinalDataList() {
        return finalDataList;
    }

    // 静态工厂方法
    public static <T> PageActuator<T> getInstance(PageConditionDef<T> pageConditionDef) {
        // 参数校验
        Objects.requireNonNull(pageConditionDef, "PageConditionDef 不能为null");

        return new PageActuator<>(pageConditionDef);
    }

    public void execute() {

        pageConditionDef.setPageConfig(pageConfig);

        // 1. 基础参数校验
        Long totalCount = pageConfig.getTotalCount();
        if (totalCount == null || totalCount <= 0) {
            Gir.log.info("无数据需处理，直接返回");
            return;
        }

        Long pageSize = pageConfig.getPageSize();
        Long maxPageNo = pageConfig.getMaxPageNo();
        long actualPageSize;
        long actualTotalPages;

        // 2. 计算实际页大小和总页数
        if (pageSize != null && pageSize > 0) {
            actualPageSize = pageSize;
            actualTotalPages = (totalCount + actualPageSize - 1) / actualPageSize;
        } else if (maxPageNo != null && maxPageNo > 0) {
            actualPageSize = (totalCount + maxPageNo - 1) / maxPageNo;
            actualTotalPages = maxPageNo;
        } else {
            throw new IllegalArgumentException(
                    "分页参数异常：pageSize（" + pageSize + "）和maxPageNo（" + maxPageNo + "）不能同时为空/小于等于0");
        }

        Consumer<T> eachRecordConsumer = pageConditionDef.getEachRecordConsumer();
        boolean isParallelConsume = pageConfig.isParallelConsumeRecordIs();

        // ========== 核心分支：消费模式 ==========
        if (isParallelConsume) {
            // 模式1：并行消费（ 边查边消费）
            parallelConsume(actualPageSize, actualTotalPages, eachRecordConsumer);
        } else {
            // 模式2：串行消费（主线程逐页查询+逐页消费，无全量堆积）
            serialConsumeByPage(actualPageSize, actualTotalPages, eachRecordConsumer);
        }
        pageConditionDef.onComplete(finalDataList);
    }

    /** 并行消费（原有逻辑，边查边消费） */
    private void parallelConsume(
            long actualPageSize, long actualTotalPages, Consumer<T> eachRecordConsumer) {
        LongStream pageNumStream = null;
        if (pageConfig.isParallelExecPageIs()) {
            pageNumStream = LongStream.range(0, actualTotalPages).parallel();
        } else {
            pageNumStream = LongStream.range(0, actualTotalPages);
        }
        pageNumStream
                .filter(pageIndex -> !isTerminate.get())
                .forEach(
                        pageIndex -> {
                            if (isTerminate.get()) return;
                            int queryPageNo =
                                    pageConfig.isPageNumStartByZero()
                                            ? (int) pageIndex
                                            : (int) (pageIndex + 1);

                            try {
                                List<T> pageRecords =
                                        pageConditionDef.getPageRecords(
                                                queryPageNo, (int) actualPageSize);
                                if (pageRecords != null && !pageRecords.isEmpty()) {
                                    pageRecords.forEach(eachRecordConsumer);
                                    if (pageConfig.isSaveResultListIs()) {
                                        finalDataList.addAll(pageRecords);
                                    }
                                }
                            } catch (Exception e) {
                                boolean isSkip =
                                        pageConditionDef.handlePageException(
                                                queryPageNo, (int) actualPageSize, e);
                                if (!isSkip) isTerminate.set(true);
                            }
                        });
    }

    /** 并行消费（原有逻辑，边查边消费） */
    private void serialConsumeByPage(
            long actualPageSize, long actualTotalPages, Consumer<T> eachRecordConsumer) {
        LongStream pageNumStream = LongStream.range(0, actualTotalPages);
        pageNumStream
                .filter(pageIndex -> !isTerminate.get())
                .forEach(
                        pageIndex -> {
                            if (isTerminate.get()) return;
                            int queryPageNo =
                                    pageConfig.isPageNumStartByZero()
                                            ? (int) pageIndex
                                            : (int) (pageIndex + 1);
                            try {
                                List<T> pageRecords =
                                        pageConditionDef.getPageRecords(
                                                queryPageNo, (int) actualPageSize);
                                if (pageRecords != null && !pageRecords.isEmpty()) {
                                    pageRecords.forEach(eachRecordConsumer);
                                    if (pageConfig.isSaveResultListIs()) {
                                        finalDataList.addAll(pageRecords);
                                    }
                                }
                            } catch (Exception e) {
                                boolean isSkip =
                                        pageConditionDef.handlePageException(
                                                queryPageNo, (int) actualPageSize, e);
                                if (!isSkip) isTerminate.set(true);
                            }
                        });
    }
}
