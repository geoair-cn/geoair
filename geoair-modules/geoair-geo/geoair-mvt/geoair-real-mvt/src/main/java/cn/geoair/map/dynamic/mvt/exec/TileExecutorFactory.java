package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.map.dynamic.adv.GirAdvQuery;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.dm.AdvExecutorDm;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.AdvExecutorMysql;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.AdvExecutorOracle;
import cn.geoair.map.dynamic.adv.query.dialect.pg.AdvExecutorPG;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;

/**
 * 矢量瓦片执行器工厂类
 * <p>
 * 根据数据源使用的数据库类型自动选择对应的瓦片执行器实现：
 * <ul>
 *   <li>PostgreSQL/PostGIS → {@link PostgisVectorTileExecutor}</li>
 *   <li>Oracle Spatial → {@link OracleVectorTileExecutor}</li>
 *   <li>Dameng → {@link OracleVectorTileExecutor}（达梦复用 Oracle 实现）</li>
 *   <li>MySQL → {@link MysqlVectorTileExecutor}</li>
 * </ul>
 */
public class TileExecutorFactory {

    private TileExecutorFactory() {}

    /**
     * 根据请求参数中的数据源自动选择对应数据库方言的矢量瓦片执行器
     *
     * @param params    瓦片请求参数（包含 dsId 和 schemaName）
     * @param layerName 图层名称
     * @return 对应该数据库的 ITileExecutor 实例
     */
    public static ITileExecutor getInstance(
            TileRequestParams params, String layerName) {
        IAdvExecutor iAdvExecutor = GirAdvQuery.getIAdvExecutor(
                params.getDsId(), params.getSchemaName());
        if (iAdvExecutor instanceof AdvExecutorPG) {
            return new PostgisVectorTileExecutor(params, layerName, iAdvExecutor);
        } else if (iAdvExecutor instanceof AdvExecutorOracle) {
            return new OracleVectorTileExecutor(params, layerName, iAdvExecutor);
        } else if (iAdvExecutor instanceof AdvExecutorDm) {
            // 达梦数据库复用 Oracle Spatial 实现
            return new OracleVectorTileExecutor(params, layerName, iAdvExecutor);
        } else if (iAdvExecutor instanceof AdvExecutorMysql) {
            return new MysqlVectorTileExecutor(params, layerName, iAdvExecutor);
        } else {
            // 未知数据库类型，默认回退到 PostGIS 实现
            return new PostgisVectorTileExecutor(params, layerName, iAdvExecutor);
        }
    }
}
