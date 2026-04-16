package cn.geoair.map.dynamic.adv.query;

import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import java.util.List;
import java.util.Map;

/**
 * 空间处理相关操作接口
 *
 * <p>核心功能：提供基于MyBatis的空间数据查询、空间字段类型识别、空间字段信息提取等核心能力， 专注于处理包含地理几何信息（点、线、面等）的数据查询与解析。 约定： 1. 所有方法名以 e
 * 开头（规避 get 前缀的命名冲突问题）； 2. SQL 语句参数统一命名为 sqlStatement，使用MyBatis风格占位符（#{参数名}）； 3.
 * 空间字段相关参数支持单字段、多字段（List）两种传入形式，适配不同业务场景。
 */
public interface IAdvGeoPreOpt extends IAdvGeoOpt {

    /**
     * 执行SQL查询并返回单行结果，自动识别结果中的所有空间字段并按指定规则处理
     *
     * <p>适用场景：明确查询结果仅返回一行，且需要自动处理所有空间字段的场景（如查询单条空间记录详情）。
     *
     * @param dynamicSql 待执行的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表（key为MyBatis占位符中的参数名，value为参数值），无参数时传空Map或null
     * @param advEnumsGeomOpt 空间字段处理策略枚举（如几何对象转WKT、转GeoJSON、仅保留几何ID等）
     * @return GirAdvOneRow 封装后的单行查询结果，包含所有字段（普通字段+处理后的空间字段）；无结果时返回null
     * @throws RuntimeException SQL执行失败、MyBatis参数绑定异常、空间字段解析异常时抛出运行时异常
     */
    GirAdvOneRow eSelectOne(
            String dynamicSql, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt);

    /**
     * 执行SQL查询并返回单行结果，指定单个空间字段并按规则处理
     *
     * <p>适用场景：仅需处理结果中某一个特定空间字段的场景（如仅提取单条记录的几何中心点字段）。
     *
     * @param dynamicSql 待执行的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @param advEnumsGeomOpt 空间字段处理策略枚举
     * @param geomFieldName 指定的空间字段名称（如 "geom"、"shape"），若字段不存在则仅处理普通字段
     * @return GirAdvOneRow 封装后的单行查询结果；无结果时返回null
     * @throws RuntimeException SQL执行失败、MyBatis参数绑定异常、指定空间字段解析异常时抛出运行时异常
     */
    GirAdvOneRow eSelectOne(
            String dynamicSql,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            String geomFieldName);

    /**
     * 执行SQL查询并返回单行结果，指定多个空间字段并按规则处理
     *
     * <p>适用场景：需要精准处理结果中多个指定空间字段的场景（如同时提取点坐标字段和面边界字段）。
     *
     * @param dynamicSql 待执行的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @param advEnumsGeomOpt 空间字段处理策略枚举
     * @param geomFieldNameList 指定的空间字段名称列表，若列表为空则等效于自动识别所有空间字段
     * @return GirAdvOneRow 封装后的单行查询结果；无结果时返回null
     * @throws RuntimeException SQL执行失败、MyBatis参数绑定异常、指定空间字段解析异常时抛出运行时异常
     */
    GirAdvOneRow eSelectOne(
            String dynamicSql,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList);

    /**
     * 执行SQL查询并返回多行结果列表，自动识别结果中的所有空间字段并按指定规则处理
     *
     * <p>适用场景：查询多条空间记录，需要批量处理所有空间字段的场景（如查询某区域内的所有点位记录）。
     *
     * @param dynamicSql 待执行的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @param advEnumsGeomOpt 空间字段处理策略枚举
     * @return List<GirAdvOneRow> 封装后的多行查询结果列表；无结果时返回空列表（非null）
     * @throws RuntimeException SQL执行失败、MyBatis参数绑定异常、空间字段解析异常时抛出运行时异常
     */
    List<GirAdvOneRow> eSelectList(
            String dynamicSql, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt);

    /**
     * 执行SQL查询并返回多行结果列表，指定单个空间字段并按规则处理
     *
     * <p>适用场景：批量查询记录，仅需处理某一个特定空间字段的场景（如批量提取多条记录的面边界字段）。
     *
     * @param dynamicSql 待执行的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @param advEnumsGeomOpt 空间字段处理策略枚举
     * @param geomFieldName 指定的空间字段名称，若字段不存在则仅处理普通字段
     * @return List<GirAdvOneRow> 封装后的多行查询结果列表；无结果时返回空列表（非null）
     * @throws RuntimeException SQL执行失败、MyBatis参数绑定异常、指定空间字段解析异常时抛出运行时异常
     */
    List<GirAdvOneRow> eSelectList(
            String dynamicSql,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            String geomFieldName);

    /**
     * 执行SQL查询并返回多行结果列表，指定多个空间字段并按规则处理
     *
     * <p>适用场景：批量查询记录，需要精准处理多个指定空间字段的场景（如批量提取点坐标和所属面边界字段）。
     *
     * @param dynamicSql 待执行的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @param advEnumsGeomOpt 空间字段处理策略枚举
     * @param geomFieldNameList 指定的空间字段名称列表，若列表为空则等效于自动识别所有空间字段
     * @return List<GirAdvOneRow> 封装后的多行查询结果列表；无结果时返回空列表（非null）
     * @throws RuntimeException SQL执行失败、MyBatis参数绑定异常、指定空间字段解析异常时抛出运行时异常
     */
    List<GirAdvOneRow> eSelectList(
            String dynamicSql,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList);

    /**
     * 解析SQL查询语句，获取结果中自动识别的首个空间字段类型
     *
     * <p>适用场景：快速判断SQL查询结果是否包含空间字段，以及该字段的类型（无需指定字段名）。
     *
     * @param dynamicSql 待解析的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @return AdvEnumsTypeGeom 自动识别的首个空间字段类型；无空间字段时返回null
     * @throws RuntimeException SQL语法错误、MyBatis参数绑定异常、数据库连接异常时抛出运行时异常
     */
    AdvEnumsTypeGeom eGetGeoTypeBySql(String dynamicSql, SqlParamMap sqlParam);

    /**
     * 解析SQL查询语句，获取指定单个空间字段的类型
     *
     * <p>适用场景：精准校验SQL查询结果中某一个空间字段的类型。
     *
     * @param dynamicSql 待解析的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @param geomFieldName 指定的空间字段名称
     * @return AdvEnumsTypeGeom 指定空间字段的类型；字段非空间字段/不存在时返回null
     * @throws RuntimeException SQL语法错误、MyBatis参数绑定异常、数据库连接异常时抛出运行时异常
     */
    AdvEnumsTypeGeom eGetGeoTypeBySql(
            String dynamicSql, SqlParamMap sqlParam, String geomFieldName);

    /**
     * 解析SQL查询语句，获取指定多个空间字段的类型
     *
     * <p>适用场景：批量校验SQL查询结果中多个空间字段的类型。
     *
     * @param dynamicSql 待解析的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @param geomFieldNames 指定的空间字段名称列表
     * @return Map<String, AdvEnumsTypeGeom> key为空间字段名称，value为对应的空间类型枚举；
     *     非空间字段/不存在的字段不会出现在返回Map中；无匹配字段时返回空Map（非null）
     * @throws RuntimeException SQL语法错误、MyBatis参数绑定异常、数据库连接异常时抛出运行时异常
     */
    Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String dynamicSql, SqlParamMap sqlParam, List<String> geomFieldNames);

    /**
     * 判断SQL查询语句的结果是否包含空间字段
     *
     * <p>适用场景：前置校验SQL是否返回空间数据，避免后续空间处理逻辑空跑。
     *
     * @param dynamicSql 待校验的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @return boolean 包含空间字段返回true，否则返回false
     * @throws RuntimeException SQL语法错误、MyBatis参数绑定异常、数据库连接异常时抛出运行时异常
     */
    boolean eIsGeomBySql(String dynamicSql, SqlParamMap sqlParam);

    /**
     * 获取SQL查询语句结果中的首个空间字段名称
     *
     * <p>适用场景：快速获取SQL返回的空间字段名称（仅需单个字段时）。
     *
     * @param dynamicSql 待解析的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @return String 首个空间字段名称；无空间字段时返回null
     * @throws RuntimeException SQL语法错误、MyBatis参数绑定异常、数据库连接异常时抛出运行时异常
     */
    String eGetGeomColumnNameBySql(String dynamicSql, SqlParamMap sqlParam);

    /**
     * 获取SQL查询语句结果中的所有空间字段名称列表
     *
     * <p>适用场景：需要批量处理所有空间字段时，先获取完整的空间字段名称列表。
     *
     * @param dynamicSql 待解析的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @return List<String> 所有空间字段名称列表；无空间字段时返回空列表（非null）
     * @throws RuntimeException SQL语法错误、MyBatis参数绑定异常、数据库连接异常时抛出运行时异常
     */
    List<String> eGetGeomColumnNameListBySql(String dynamicSql, SqlParamMap sqlParam);

    /**
     * 获取SQL查询语句结果中的所有空间字段的完整信息（包含schema、字段名、类型等）
     *
     * <p>适用场景：需要空间字段的完整元数据，而非仅名称时。
     *
     * @param dynamicSql 待解析的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @return List<FieldBySchemaApo> 所有空间字段的元数据列表；无空间字段时返回空列表（非null）
     * @throws RuntimeException SQL语法错误、MyBatis参数绑定异常、数据库连接异常时抛出运行时异常
     */
    List<FieldBySchemaApo> eGetGeomColumnListBySql(String dynamicSql, SqlParamMap sqlParam);

    /**
     * 获取SQL查询语句结果中的首个空间字段的完整信息（包含schema、字段名、类型等）
     *
     * <p>适用场景：仅需首个空间字段的完整元数据时。
     *
     * @param dynamicSql 待解析的SQL查询语句，使用MyBatis风格占位符（#{参数名}）传递参数 sqlParam
     *     SQL语句的参数映射表，无参数时传空Map或null
     * @return FieldBySchemaApo 首个空间字段的元数据；无空间字段时返回null
     * @throws RuntimeException SQL语法错误、MyBatis参数绑定异常、数据库连接异常时抛出运行时异常
     */
    FieldBySchemaApo eGetGeomColumnBySql(String dynamicSql, SqlParamMap sqlParam);
}
