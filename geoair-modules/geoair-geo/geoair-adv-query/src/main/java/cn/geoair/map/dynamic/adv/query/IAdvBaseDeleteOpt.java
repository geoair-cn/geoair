package cn.geoair.map.dynamic.adv.query;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;

import java.util.Map;
import java.util.Set;

/**
 * 数据删除相关的基础操作接口
 *
 * <p>覆盖单条删除、批量删除、自定义SQL删除、条件删除、逻辑删除、防误删保护等全场景， 适配PostgreSQL/MySQL等主流数据库，兼顾性能与数据安全
 */
public interface IAdvBaseDeleteOpt extends IAdvConfigOpt {

    /**
     * 设置数据源获取器
     *
     * @param dataSourceGetter 数据源获取器，用于获取数据库连接
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);
    // ==================== 自定义SQL删除（最灵活） ====================

    /**
     * 执行自定义删除SQL语句（无参数）
     *
     * <p>支持任意复杂的DELETE SQL（含多表关联删除、子查询删除等）
     *
     * @param sqlStatement 自定义删除SQL语句（支持MyBatis标签） <br>
     *                     示例：DELETE FROM user WHERE create_time < '2024-01-01' AND status = 0
     * @return Integer 受影响的行数
     */
    Integer bDeleteBySql(String sqlStatement);

    /**
     * 执行带参数的自定义删除SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param sqlStatement 自定义删除SQL语句（含参数占位符） <br>
     *                     示例：DELETE FROM user WHERE dept_id = #{deptId} AND age > #{age} sqlParam
     *                     SQL参数映射（key为占位符名称，value为参数值） <br>
     *                     示例：{ "deptId": 5, "age": 60 }
     * @return Integer 受影响的行数
     */
    Integer bDeleteBySql(String sqlStatement, SqlParamMap sqlParam);

    // ==================== 主键删除（最常用） ====================

    /**
     * 根据单个主键值删除数据
     *
     * <p>适用于按主键删除单行数据的场景，性能最优
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名（如：id）
     * @param id        主键值
     * @return Integer 受影响的行数（成功返回1，无匹配数据返回0）
     */
    Integer bDeleteByPK(String tableName, String idKey, Object id);

    /**
     * 根据主键集合批量删除数据
     *
     * <p>底层优化为IN查询（DELETE FROM ... WHERE id IN (?, ?, ...)）， 适用于批量删除少量数据（建议不超过1000条）
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param ids       主键值集合
     * @return Integer 受影响的行数
     */
    Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids);

    /**
     * 分批次批量删除主键数据（避免IN子句参数过多）
     *
     * <p>自动将主键集合按批次拆分，适用于删除大量数据（如1万+条）
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param ids       主键值集合
     * @param batchSize 每批次删除的主键数量（建议1000-5000）
     * @return Integer 受影响的总行数
     */
    Integer bDeleteByPKs(
            String tableName, String idKey, Set<Object> ids, int batchSize);

    // ==================== 条件删除 ====================

    /**
     * 按条件删除数据（Map格式条件）
     *
     * <p>条件拼接为AND关系，支持多字段组合条件，**禁止传入空条件（避免全表删除）**
     *
     * @param tableName 目标表名
     * @param whereMap  删除条件（key=字段名，value=条件值） <br>
     *                  示例：{ "dept_id": 5, "status": 0 }
     * @return Integer 受影响的行数
     */
    Integer bDeleteByMap(String tableName, Map<String, Object> whereMap);

    /**
     * 按条件批量删除（分批次执行）
     *
     * <p>适用于删除大量数据，避免单次删除锁表时间过长
     *
     * @param tableName 目标表名
     * @param whereMap  删除条件
     * @param batchSize 每批次删除的行数（建议1000-5000）
     * @return Integer 受影响的总行数
     */
    Integer bDeleteByMap(String tableName, Map<String, Object> whereMap, int batchSize);

    // ==================== where 条件的删除 ====================


//    <T> Integer bDeleteByWhere(String tableName,   GirAdvWhereLambdaFilter<T> whereFilter);
//
//    <T> Integer bDeleteByWhere(String tableName,  GirAdvWhereFilter whereFilter);


}
