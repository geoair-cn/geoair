package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * V3 JDBC 图层输入配置。
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class V3JdbcInputConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JDBC 数据源。 */
    private DataSourceConfig dataSource;

    /** 作为切片输入的查询语句或子查询。 */
    private String queryStatement;

    /** 数据库读取策略。 */
    private ReadStrategy readStrategy = ReadStrategy.ID_PAGE;

    /** 最大读取分区数。 */
    private Integer maxPartitionNum = 20;
}
