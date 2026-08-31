package cn.geoair.map.dynamic.tools.page;

import cn.geoair.base.Gir;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.LongStream;

/**
 * 可配置的分页查询执行器。
 *
 * <p>执行器为一次性对象：首次调用 {@link #getFinalDataList()} 会触发 {@link #execute()}， 后续调用不会再次查询。是否并行执行页面、是否收集结果由
 * {@link PageConfig} 控制。
 *
 * @param <T> 单条记录类型
 * @author 张逢吉
 */
public class PageActuator<T> {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private final PageConditionDef<T> pageConditionDef;

    /** 当前任务的分页配置，由 {@link PageConditionDef#setPageConfig(PageConfig)} 初始化。 */
    PageConfig pageConfig = new PageConfig();

    boolean executeIs = false;

    private final List<T> finalDataList = new ArrayList<T>();

    // 仅用于标记是否终止
    private final AtomicBoolean isTerminate = new AtomicBoolean(false);

    /** 私有构造器，使用 {@link #getInstance(PageConditionDef)} 创建。 */
    private PageActuator(PageConditionDef<T> pageConditionDef) {
        this.pageConditionDef = pageConditionDef;
    }

    /**
     * 创建新的分页执行器。
     *
     * @param pageConditionDef 分页查询与消费定义
     * @param <T> 单条记录类型
     * @return 独立的分页执行器
     * @throws NullPointerException 分页定义为空时抛出
     */
    public static <T> PageActuator<T> getInstance(PageConditionDef<T> pageConditionDef) {
        // 参数校验
        Objects.requireNonNull(pageConditionDef, "PageConditionDef 不能为null");

        return new PageActuator<>(pageConditionDef);
    }

    /**
     * 获取已收集的结果；首次调用会自动执行分页任务。
     *
     * @return 结果列表；未开启结果保存时通常为空
     */
    public List<T> getFinalDataList() {
        if (!executeIs) {
            execute();
        }
        return finalDataList;
    }

    /**
     * 执行分页任务。
     *
     * @return 当前执行器，便于链式调用
     */
    public PageActuator<T> execute() {
        executeIs = true;
        pageConditionDef.setPageConfig(pageConfig);

        // 1. 基础参数校验
        Long totalCount =
                pageConditionDef.getTotalRecordCount() == null
                        ? pageConfig.getTotalCount()
                        : pageConditionDef.getTotalRecordCount();
        if (totalCount == null || totalCount <= 0) {
            Gir.log.info("无数据需处理，直接返回");
            return this;
        }

        Long pageSize = pageConfig.getPageSize();
        Long maxPageNo = pageConfig.getMaxPageNo();
        long actualPageSize;
        long actualTotalPages;

        // 2. 计算实际页大小和总页数
        boolean byPageSize = false;

        if (pageSize != null && pageSize > 0) {
            byPageSize = true;

        } else if (maxPageNo != null && maxPageNo > 0) {

        } else {
            pageSize = 25L;
            log.info("由于没有设置maxPageNo 与pageSize 这里对 pageSize 进行设置默认值为25");
            byPageSize = true;
        }
        if (byPageSize) { // 通过每页大小计算总页数，进行每页遍历。数据量大的时候可能有很多页
            actualPageSize = pageSize;
            actualTotalPages = (totalCount + actualPageSize - 1) / actualPageSize;
        } else { // 通过总页数反着设置每页大小，防止数据量大的时候 ，频繁访问数据库
            if (totalCount <= maxPageNo) {
                actualPageSize = totalCount;
                actualTotalPages = 1;
            } else {
                actualPageSize = (totalCount + maxPageNo - 1) / maxPageNo;
                if (actualPageSize < 25) { // 如果每页条数少于25条，那么就按照25条每页进行分页
                    actualPageSize = 25;
                    actualTotalPages = (totalCount + actualPageSize - 1) / actualPageSize;
                } else {
                    actualTotalPages = maxPageNo;
                }
            }
        }
        AtomicLong count = new AtomicLong(0);
        Consumer<T> wapperConsumer =
                new Consumer<T>() {
                    @Override
                    public void accept(T t) {
                        count.incrementAndGet();
                        pageConditionDef.getEachRecordConsumer().accept(t);
                    }
                };

        boolean isParallelConsume = pageConfig.isParallelConsumeRecordIs();

        // ========== 核心分支：消费模式 ==========
        if (isParallelConsume) {
            // 模式1：并行消费（ 边查边消费）
            parallelConsume(actualPageSize, actualTotalPages, wapperConsumer);
        } else {
            // 模式2：串行消费（主线程逐页查询+逐页消费，无全量堆积）
            serialConsumeByPage(actualPageSize, actualTotalPages, wapperConsumer);
        }
        pageConditionDef.onComplete(
                finalDataList, actualPageSize, actualTotalPages, count.get(), totalCount);
        return this;
    }

    /** 根据配置并行或串行查询页面，并在查询到页面后立即消费记录。 */
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

    /** 串行查询页面，并在查询到页面后立即消费记录。 */
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

    public static void main(String[] args) {
        GirAdvTools.getPageActuatorOpt(
                        new PageConditionDef<Object>() {
                            @Override
                            public Long getTotalRecordCount() {
                                return 11L;
                            }

                            @Override
                            public void setPageConfig(PageConfig pageConfig) {
                                pageConfig.setMaxPageNo(20L);
                            }

                            @Override
                            public List<Object> getPageRecords(Integer pageNo, Integer pageSize) {
                                System.out.println(pageNo + "----" + pageSize);
                                List<Object> pageRecords = new ArrayList<>();
                                for (Integer i = 0; i < pageSize; i++) {
                                    pageRecords.add(new Object());
                                }
                                return pageRecords;
                            }
                        })
                .execute();
    }
}
