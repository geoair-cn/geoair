package cn.geoair.map.dynamic.adv.query;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/10/9 11:07 @description： 集合执行器，空接口，只继承上游接口
 */
public interface IAdvExecutor
        extends IDataSourceGetter,
        IAdvBaseOpt,
        IAdvDDLOpt,
        IAdvGeoOpt,
        IAdvGeoPreOpt,
        IAdvSimplePageOpt,
        IAdvSimplePagePreOpt,
        IAdvWhereSelectOpt,
        DialectTableNameProcessor {

    /**
     * 获取用于SQL的Schema名（含默认值与基础校验）
     *
     * @return 处理后的Schema名
     */
    String tbGetSchemaNameForSql();

    /**
     * （默认Schema）获取带Schema的完整表名
     *
     * @param tableName 原始表名（可含Schema，也可不含）
     * @return 带Schema的标准表名（如 "public"."user"）
     */
    String tbGetTableNameWithSchema(String tableName);

    /**
     * （指定Schema）获取带Schema的完整表名
     *
     * @param tableName  原始表名（可含Schema，也可不含）
     * @param schemaName 指定的Schema名（优先级低于表名中提取的Schema）
     * @return 带Schema的标准表名
     */
    String tbGetTableNameWithSchema(String tableName, String schemaName);


}
