package cn.geoair.map.dynamic.adv.query;

import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.queryr.QueryRequestBuilder;
import java.util.List;
import java.util.function.Consumer;

/**
 * 动态查询接口
 *
 * <p>自动组装WHERE条件的查询，支持链式条件构建、分页、排序等功能
 *
 * <p>使用{@link GirAdvQueryRequest}封装查询参数
 *
 * @author zhangjun
 * @date Created in 2026/4/16 09:28
 */
public interface IAdvWhereSelectOpt extends IAdvConfigOpt {

    /**
     * 查询单条记录
     *
     * <p>根据查询条件返回第一条匹配的记录，适用于根据唯一条件查询的场景
     *
     * @param query 查询请求对象，封装了表名、字段、条件、排序等参数
     * @return 单行查询结果，封装了列名与对应值的映射关系；无结果时返回null
     */
    GirAdvOneRow wSelectOne(GirAdvQueryRequest query);

    /**
     * 查询单条记录（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 单行查询结果
     */
    <T> GirAdvOneRow wSelectOne(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询单条记录（泛型版本）
     *
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 单行查询结果
     */
    <T> GirAdvOneRow wSelectOne(Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询多条记录列表
     *
     * <p>根据查询条件返回所有匹配的记录列表，适用于常规列表查询场景
     *
     * @param query 查询请求对象，封装了表名、字段、条件、排序等参数
     * @return 多行查询结果列表，每个元素对应一行数据的列值映射；无结果时返回空列表
     */
    List<GirAdvOneRow> wSelectList(GirAdvQueryRequest query);

    /**
     * 查询多条记录列表（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 多行查询结果列表
     */
    <T> List<GirAdvOneRow> wSelectList(
            Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询多条记录列表（泛型版本）
     *
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 多行查询结果列表
     */
    <T> List<GirAdvOneRow> wSelectList(Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 分页查询
     *
     * <p>根据查询条件返回分页结果，包含总记录数和当前页数据，适用于列表分页展示场景
     *
     * @param query 查询请求对象，封装了表名、字段、条件、排序、分页等参数
     * @return 分页结果对象，包含数据列表、总记录数、当前页码、每页大小等信息
     */
    PageApo<GirAdvOneRow> wSelectPage(GirAdvQueryRequest query);

    /**
     * 分页查询（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 分页结果对象
     */
    <T> PageApo<GirAdvOneRow> wSelectPage(
            Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 分页查询（泛型版本）
     *
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 分页结果对象
     */
    <T> PageApo<GirAdvOneRow> wSelectPage(Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 流式查询多条记录
     *
     * <p>通过Consumer逐行处理数据，避免一次性加载大量数据到内存导致OOM，适用于大数据量导出或批处理场景
     *
     * <p>注意：流式查询会保持数据库连接直到所有数据处理完成，使用完毕后会自动释放资源
     *
     * @param query 查询请求对象，封装了表名、字段、条件、排序等参数
     * @param rowConsumer 行数据消费器，用于逐行处理查询结果中的每一条GirAdvOneRow数据 示例：row -> log.info("数据：" +
     *     row.get("id"))
     */
    void wSelectStream(GirAdvQueryRequest query, Consumer<GirAdvOneRow> rowConsumer);

    /**
     * 流式查询多条记录（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型
     * @param consumer 查询构建器消费者
     * @param rowConsumer 行数据消费器
     * @param <T> 实体类型
     */
    <T> void wSelectStream(
            Class<T> entityClass,
            Consumer<QueryRequestBuilder<T>> consumer,
            Consumer<GirAdvOneRow> rowConsumer);

    /**
     * 流式查询多条记录（泛型版本）
     *
     * @param consumer 查询构建器消费者
     * @param rowConsumer 行数据消费器
     * @param <T> 实体类型
     */
    <T> void wSelectStream(
            Consumer<QueryRequestBuilder<T>> consumer, Consumer<GirAdvOneRow> rowConsumer);

    /**
     * 查询记录总数
     *
     * <p>根据查询条件返回匹配的记录总数，适用于分页查询前获取总记录数
     *
     * @param query 查询请求对象，封装了表名、条件等参数（分页、排序参数会被忽略）
     * @return 匹配条件的记录总数；无结果时返回0
     */
    Number wSelectCount(GirAdvQueryRequest query);

    /**
     * 查询记录总数（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 记录总数
     */
    <T> Number wSelectCount(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询记录总数（泛型版本）
     *
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 记录总数
     */
    <T> Number wSelectCount(Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询并返回纯值列表结果
     *
     * <p>外层List代表多行数据，内层List代表单行数据的所有列值（按查询列的顺序排列）， 相当于将GirAdvOneRow中的value按顺序提取到List中，减少对象封装开销
     *
     * <p>适用于只需要数值、不需要列名映射的场景，如统计报表数据导出
     *
     * @param query 查询请求对象，封装了表名、字段、条件、排序等参数
     * @return 纯值列表结果，例如：[[1, "张三", 25], [2, "李四", 28]]；无结果时返回空列表
     */
    List<List<Object>> wSelectListToValueList(GirAdvQueryRequest query);

    /**
     * 查询并返回纯值列表结果（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 纯值列表结果
     */
    <T> List<List<Object>> wSelectListToValueList(
            Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询并返回纯值列表结果（泛型版本）
     *
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 纯值列表结果
     */
    <T> List<List<Object>> wSelectListToValueList(Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 执行聚合查询并返回单个数值结果
     *
     * <p>适用于COUNT、SUM、AVG、MAX、MIN等聚合函数查询，返回单个数值结果
     *
     * <p>注意：查询请求中的fields字段应设置为聚合表达式，如"COUNT(*)"、"SUM(amount)"
     *
     * @param query 查询请求对象，封装了表名、聚合字段、条件等参数
     * @return 聚合查询的数值结果（Integer/Long/BigDecimal等）；无结果时返回null
     */
    Number wSelectNumber(GirAdvQueryRequest query);

    /**
     * 执行聚合查询并返回单个数值结果（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 聚合查询的数值结果
     */
    <T> Number wSelectNumber(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 执行聚合查询并返回单个数值结果（泛型版本）
     *
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 聚合查询的数值结果
     */
    <T> Number wSelectNumber(Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询并将单行结果映射为指定类型的Java对象
     *
     * <p>自动将查询结果的列值映射到目标类的属性（字段名需匹配），支持基本类型、String、日期等类型转换
     *
     * <p>适用于需要将查询结果直接转换为业务对象的场景
     *
     * @param query 查询请求对象，封装了表名、字段、条件等参数
     * @param clazz 目标对象的类型（如User.class）
     * @param <E> 目标对象的泛型类型
     * @return 映射后的单行Java对象；无结果时返回null
     */
    <E> E wSelectObjOne(GirAdvQueryRequest query, Class<E> clazz);

    /**
     * 查询并将单行结果映射为指定类型的Java对象（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型（用于构建查询条件）
     * @param resultClass 结果对象类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @param <R> 结果类型
     * @return 映射后的单行Java对象
     */
    <T, R> R wSelectObjOne(
            Class<T> entityClass, Class<R> resultClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询并将单行结果映射为指定类型的Java对象（泛型版本）
     *
     * @param entityClass 结果对象类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 映射后的单行Java对象
     */
    <T> T wSelectObjOne(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询并将多行结果映射为指定类型的Java对象列表
     *
     * <p>适合小数据量场景，结果一次性映射为对象列表，字段名需与目标类属性匹配
     *
     * <p>适用于需要将查询结果批量转换为业务对象列表的场景
     *
     * @param query 查询请求对象，封装了表名、字段、条件、排序等参数
     * @param clazz 目标对象的类型（如User.class）
     * @param <E> 目标对象的泛型类型
     * @return 映射后的多行Java对象列表；无结果时返回空列表
     */
    <E> List<E> wSelectObjList(GirAdvQueryRequest query, Class<E> clazz);

    /**
     * 查询并将多行结果映射为指定类型的Java对象列表（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型（用于构建查询条件）
     * @param resultClass 结果对象类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @param <R> 结果类型
     * @return 映射后的多行Java对象列表
     */
    <T, R> List<R> wSelectObjList(
            Class<T> entityClass, Class<R> resultClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 查询并将多行结果映射为指定类型的Java对象列表（泛型版本）
     *
     * @param entityClass 结果对象类型
     * @param consumer 查询构建器消费者
     * @param <T> 实体类型
     * @return 映射后的多行Java对象列表
     */
    <T> List<T> wSelectObjList(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer);

    /**
     * 流式查询并将结果逐行映射为指定类型的Java对象
     *
     * <p>适合大数据量对象映射场景，逐行读取并处理，避免内存溢出，字段名需与目标类属性匹配
     *
     * <p>适用于需要逐行处理大量业务对象的场景，如数据同步、批量导出等
     *
     * @param query 查询请求对象，封装了表名、字段、条件、排序等参数
     * @param clazz 目标对象的类型（如Employee.class）
     * @param rowConsumer 行对象消费器，用于逐行处理映射后的Java对象 示例：emp -> log.info("员工姓名：" + emp.getName())
     * @param <E> 目标对象的泛型类型
     */
    <E> void wSelectObjStream(GirAdvQueryRequest query, Class<E> clazz, Consumer<E> rowConsumer);

    /**
     * 流式查询并将结果逐行映射为指定类型的Java对象（泛型版本-带实体类）
     *
     * @param entityClass 实体类类型（用于构建查询条件）
     * @param resultClass 结果对象类型
     * @param consumer 查询构建器消费者
     * @param rowConsumer 行对象消费器
     * @param <T> 实体类型
     * @param <R> 结果类型
     */
    <T, R> void wSelectObjStream(
            Class<T> entityClass,
            Class<R> resultClass,
            Consumer<QueryRequestBuilder<T>> consumer,
            Consumer<R> rowConsumer);

    /**
     * 流式查询并将结果逐行映射为指定类型的Java对象（泛型版本）
     *
     * @param entityClass 结果对象类型
     * @param consumer 查询构建器消费者
     * @param rowConsumer 行对象消费器
     * @param <T> 实体类型
     */
    <T> void wSelectObjStream(
            Class<T> entityClass,
            Consumer<QueryRequestBuilder<T>> consumer,
            Consumer<T> rowConsumer);
}
