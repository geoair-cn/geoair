package cn.geoair.map.dynamic.adv.query;

import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;

import java.util.List;
import java.util.function.Consumer;

/**
 * 查询的基础操作接口
 *
 * <p>设计初衷模仿MyBatis-Plus的BaseMapper，提供通用的数据库查询操作封装， 支持传入带MyBatis标签的SQL语句，适配动态查询场景下的各类基础查询需求
 */
public interface IAdvBaseSelectOpt {

    /**
     * 设置数据源获取器
     *
     * @param dataSourceGetter 数据源获取器，用于获取数据库连接
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);

    /**
     * 执行查询并返回单行结果（无参数版）
     *
     * <p>如果查询结果有多行，仅返回第一行；无结果时返回null
     *
     * @param sql 待执行的SQL语句（支持MyBatis标签，无需传入参数） <br>
     *     示例：SELECT id, name, age FROM user WHERE id = 1001
     * @return GirAdvOneRow 单行查询结果，封装了列名与对应值的映射关系；无结果时返回null
     */
    GirAdvOneRow bSelectOne(String sql);

    /**
     * 执行查询并返回多行结果列表（无参数版）
     *
     * <p>适合查询结果集较小的场景，结果一次性加载到内存中
     *
     * @param sql 待执行的SQL语句（支持MyBatis标签，无需传入参数） <br>
     *     示例：SELECT id, name FROM dept WHERE status = 1
     * @return List<GirAdvOneRow> 多行查询结果列表，每个元素对应一行数据的列值映射；无结果时返回空列表
     */
    List<GirAdvOneRow> bSelectList(String sql);

    /**
     * 流式查询多行结果（无参数版），通过Consumer逐行处理数据
     *
     * <p>适合大数据量查询场景，避免一次性加载所有数据到内存导致OOM，逐行读取并处理
     *
     * @param sql 待执行的SQL语句（支持MyBatis标签，无需传入参数） <br>
     *     示例：SELECT id, log_content FROM system_log WHERE create_time >= '2024-01-01'
     * @param rowConsumer 行数据消费器，用于逐行处理查询结果中的每一条GirAdvOneRow数据 <br>
     *     示例：row -> Gir.log.info("日志ID：" + row.get("id") + " 内容：" + row.get("log_content"))
     */
    void bSelectList(String sql, Consumer<GirAdvOneRow> rowConsumer);

    /**
     * 执行查询并返回多行纯值列表结果（无参数版），仅保留值无列名
     *
     * <p>外层List代表多行数据，内层List代表单行数据的所有列值（按查询列的顺序排列）， 相当于将GirAdvOneRow中的value按顺序提取到List中，减少对象封装开销
     *
     * @param sql 待执行的SQL语句（支持MyBatis标签，无需传入参数） <br>
     *     示例：SELECT id, name, age FROM user WHERE dept_id = 5
     * @return List<List < Object>> 纯值列表结果，例如：[[1, "张三", 25], [2, "李四", 28]]；无结果时返回空列表
     */
    List<List<Object>> bSelectListToValueList(String sql);

    /**
     * 执行聚合查询并返回单个数值结果（无参数版）
     *
     * <p>适用于COUNT、SUM、AVG、MAX、MIN等聚合函数查询，返回单个数值结果
     *
     * @param sql 待执行的聚合SQL语句（支持MyBatis标签，无需传入参数） <br>
     *     示例1：SELECT COUNT(*) FROM user <br>
     *     示例2：SELECT SUM(salary) FROM employee WHERE position = '工程师'
     * @return Number 聚合查询的数值结果（Integer/Long/BigDecimal等）；无结果时返回null
     */
    Number bSelectNumber(String sql);

    /**
     * 查询SQL执行结果的总行数（无参数版），适配分页场景的count查询
     *
     * <p>内部会将原SQL包装为COUNT(1)查询，无需手动编写count语句，简化分页逻辑
     *
     * @param sql 待统计行数的原始查询SQL（支持MyBatis标签，无需传入参数） <br>
     *     示例：SELECT id, name FROM user WHERE name LIKE '%张三%'
     * @return Number 结果集的总行数（Long类型）；无结果时返回0
     */
    Number bSelectRecordRowCount(String sql);

    /**
     * 执行查询并将单行结果映射为指定类型的Java对象（无参数版）
     *
     * <p>自动将查询结果的列值映射到目标类的属性（字段名需匹配），支持基本类型、String、日期等类型转换
     *
     * @param sql 待执行的SQL语句（支持MyBatis标签，无需传入参数） <br>
     *     示例：SELECT id, name, age FROM user WHERE id = 1001
     * @param clazz 目标对象的类型（如User.class）
     * @param <E> 目标对象的泛型类型
     * @return E 映射后的单行Java对象；无结果时返回null
     */
    <E> E bSelectObjOne(String sql, Class<E> clazz);

    /**
     * 执行查询并将多行结果映射为指定类型的Java对象列表（无参数版）
     *
     * <p>适合小数据量场景，结果一次性映射为对象列表，字段名需与目标类属性匹配
     *
     * @param sql 待执行的SQL语句（支持MyBatis标签，无需传入参数） <br>
     *     示例：SELECT id, name, dept_id FROM user WHERE dept_id = 5
     * @param clazz 目标对象的类型（如User.class）
     * @param <E> 目标对象的泛型类型
     * @return List<E> 映射后的多行Java对象列表；无结果时返回空列表
     */
    <E> List<E> bSelectObjList(String sql, Class<E> clazz);

    /**
     * 流式查询并将结果逐行映射为指定类型的Java对象（无参数版）
     *
     * <p>适合大数据量对象映射场景，逐行读取并处理，避免内存溢出，字段名需与目标类属性匹配
     *
     * @param sql 待执行的SQL语句（支持MyBatis标签，无需传入参数） <br>
     *     示例：SELECT id, name, salary FROM employee WHERE dept_id = 5
     * @param clazz 目标对象的类型（如Employee.class）
     * @param rowConsumer 行对象消费器，用于逐行处理映射后的Java对象 <br>
     *     示例：emp -> Gir.log.info("员工姓名：" + emp.getName() + " 薪资：" + emp.getSalary())
     * @param <E> 目标对象的泛型类型
     */
    <E> void bSelectObjList(String sql, Class<E> clazz, Consumer<E> rowConsumer);

    /**
     * 执行查询并返回单行结果
     *
     * @param sqlStatement 待执行的SQL语句（支持MyBatis标签） <br>
     *     示例1（普通SQL）：SELECT id, name, age FROM user WHERE id = #{userId} <br>
     *     示例2（带MyBatis标签）： SELECT id, name, age FROM user <where> <if test="name != null">AND name
     *     LIKE CONCAT('%', #{name}, '%')</if> <if test="age != null">AND age = #{age}</if> </where>
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "userId": 1001, "name": "张三", "age": 25 }
     * @return GirAdvOneRow 单行查询结果，封装了列名与对应值的映射关系
     */
    GirAdvOneRow bSelectOne(String sqlStatement, SqlParamMap sqlParam);

    /**
     * 执行查询并返回多行结果列表
     *
     * @param sqlStatement 待执行的SQL语句（支持MyBatis标签） <br>
     *     示例1（普通SQL）：SELECT id, name FROM dept WHERE status = #{status} <br>
     *     示例2（带MyBatis标签）： SELECT id, name, create_time FROM dept <where> <if test="status !=
     *     null">AND status = #{status}</if> <if test="createTimeStart != null">AND create_time >=
     *     #{createTimeStart}</if> </where> ORDER BY create_time DESC
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "status": 1, "createTimeStart": "2024-01-01" }
     * @return List<GirAdvOneRow> 多行查询结果列表，每个元素对应一行数据的列值映射
     */
    List<GirAdvOneRow> bSelectList(String sqlStatement, SqlParamMap sqlParam);

    /**
     * 流式查询多行结果，通过Consumer逐行处理数据（避免一次性加载大量数据占用内存）
     *
     * @param sqlStatement 待执行的SQL语句（支持MyBatis标签） <br>
     *     示例（大数据量查询）： SELECT id, log_content, create_time FROM system_log WHERE create_time BETWEEN
     *     #{startTime} AND #{endTime}
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "startTime": "2024-01-01 00:00:00", "endTime": "2024-01-31 23:59:59" }
     * @param rowConsumer 行数据消费器，用于逐行处理查询结果中的每一条GirAdvOneRow数据 <br>
     *     示例：row -> Gir.log.info("日志ID：" + row.get("id") + " 内容：" + row.get("log_content"))
     */
    void bSelectList(String sqlStatement, SqlParamMap sqlParam, Consumer<GirAdvOneRow> rowConsumer);

    /**
     * 执行查询并返回多行结果，结果以纯值列表形式封装（仅保留值，无列名）
     *
     * <p>外层List代表多行数据，内层List代表单行数据的所有列值（按查询列的顺序排列）， 相当于将GirAdvOneRow中的value按顺序提取到List中
     *
     * @param sqlStatement 待执行的SQL语句（支持MyBatis标签） <br>
     *     示例：SELECT id, name, age FROM user WHERE dept_id = #{deptId}
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "deptId": 5 }
     * @return List<List < Object>> 纯值列表结果，例如：[[1, "张三", 25], [2, "李四", 28]]
     */
    List<List<Object>> bSelectListToValueList(String sqlStatement, SqlParamMap sqlParam);

    /**
     * 执行聚合查询并返回单个数值结果（如COUNT、SUM、AVG等）
     *
     * @param sqlStatement 待执行的聚合SQL语句（支持MyBatis标签） <br>
     *     示例1：SELECT COUNT(*) FROM user WHERE dept_id = #{deptId} <br>
     *     示例2：SELECT SUM(salary) FROM employee WHERE position = #{position}
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "deptId": 5, "position": "工程师" }
     * @return Number 聚合查询的数值结果（Integer/Long/BigDecimal等）
     */
    Number bSelectNumber(String sqlStatement, SqlParamMap sqlParam);

    /**
     * 查询SQL执行结果的总行数（适配分页场景的count查询）
     *
     * @param sqlStatement 待执行的count SQL语句（支持MyBatis标签） <br>
     *     示例：SELECT COUNT(*) FROM user <where> <if test="name != null">AND name LIKE CONCAT('%',
     *     #{name}, '%')</if> </where>
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "name": "张三" }
     * @return Number 结果集的总行数（Long类型）
     */
    Number bSelectRecordRowCount(String sqlStatement, SqlParamMap sqlParam);

    /**
     * 执行查询并将单行结果映射为指定类型的Java对象
     *
     * @param sqlStatement 待执行的SQL语句（支持MyBatis标签） <br>
     *     示例：SELECT id, name, age FROM user WHERE id = #{userId}
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "userId": 1001 }
     * @param clazz 目标对象的类型（如User.class）
     * @param <E> 目标对象的泛型类型
     * @return E 映射后的单行Java对象
     */
    <E> E bSelectObjOne(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz);

    /**
     * 执行查询并将多行结果映射为指定类型的Java对象列表
     *
     * @param sqlStatement 待执行的SQL语句（支持MyBatis标签） <br>
     *     示例：SELECT id, name, dept_id FROM user WHERE dept_id = #{deptId}
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "deptId": 5 }
     * @param clazz 目标对象的类型（如User.class）
     * @param <E> 目标对象的泛型类型
     * @return List<E> 映射后的多行Java对象列表
     */
    <E> List<E> bSelectObjList(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz);

    /**
     * 流式查询并将结果逐行映射为指定类型的Java对象，通过Consumer处理（避免内存溢出）
     *
     * @param sqlStatement 待执行的SQL语句（支持MyBatis标签） <br>
     *     示例：SELECT id, name, salary FROM employee WHERE dept_id = #{deptId}
     * @param sqlParam SQL语句中的参数映射（key为参数名，value为参数值） <br>
     *     示例：{ "deptId": 5 }
     * @param clazz 目标对象的类型（如Employee.class）
     * @param rowConsumer 行对象消费器，用于逐行处理映射后的Java对象 <br>
     *     示例：emp -> Gir.log.info("员工姓名：" + emp.getName() + " 薪资：" + emp.getSalary())
     * @param <E> 目标对象的泛型类型
     */
    <E> void bSelectObjList(
            String sqlStatement, SqlParamMap sqlParam, Class<E> clazz, Consumer<E> rowConsumer);
}
