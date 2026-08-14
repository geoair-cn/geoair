package cn.geoair.map.dynamic.statics.mvt.spark.vectile.test;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.ProtocolUrl;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.hutool.core.util.IdUtil;

/**
 * TileSliceParameter 示例：演示自定义协议解析、序列化、反序列化。
 */
public class TileSliceParameterExample {

    public static void main(String[] args) {
//        System.out.println("========== JdbcUrl 协议解析测试 ==========\n");
//        testJdbcUrlParser();

        System.out.println("\n========== TileSliceParameter 序列化 / 反序列化测试 ==========\n");
        testTileSliceParameter();

//        System.out.println("\n========== 多数据库协议格式测试 ==========\n");
//        testMultiDatabaseProtocol();
    }

    /**
     * 测试 JdbcUrl 自定义协议解析
     */
    private static void testJdbcUrlParser() {
        // PostgreSQL：完整格式
        String pgUrl = "#jdbc:postgresql://postgres#tcsd1234/116.198.227.117:35432/address/test1/big_mian";
        ProtocolUrl pg = new ProtocolUrl(pgUrl);
        System.out.println("[PostgreSQL 完整格式]");
        System.out.println("  输入:     " + pgUrl);
        System.out.println("  toString: " + pg);
        System.out.println("  jdbcUrl:  " + pg.toJdbcUrl());
        System.out.println("  username: " + pg.getUsername());
        System.out.println("  password: " + pg.getPassword());
        System.out.println("  host:     " + pg.getHost());
        System.out.println("  port:     " + pg.getPort());
        System.out.println("  database: " + pg.getDatabase());
        System.out.println("  schema:   " + pg.getSchema());
        System.out.println("  table:    " + pg.getTableName());
        System.out.println("  tableForSql: " + pg.getTableForSql());
        System.out.println();

        // PostgreSQL：无表名
        ProtocolUrl pg2 = new ProtocolUrl("#jdbc:postgresql://postgres#secret/10.0.0.1:5432/mydb/public");
        System.out.println("[PostgreSQL 无表名]");
        System.out.println("  jdbcUrl: " + pg2.toJdbcUrl());
        System.out.println("  schema:  " + pg2.getSchema());
        System.out.println("  table:   " + pg2.getTableName());
        System.out.println();

        // MySQL：无 schema，用空段占位
        String mysqlUrl = "#jdbc:mysql://root#mypwd/192.168.1.100:3306/gisdb//tile_cache";
        ProtocolUrl mysql = new ProtocolUrl(mysqlUrl);
        System.out.println("[MySQL 无 schema，空段占位]");
        System.out.println("  输入:     " + mysqlUrl);
        System.out.println("  toString: " + mysql);
        System.out.println("  jdbcUrl:  " + mysql.toJdbcUrl());
        System.out.println("  database: " + mysql.getDatabase());
        System.out.println("  schema:   " + mysql.getSchema());
        System.out.println("  table:    " + mysql.getTableName());
        System.out.println("  tableForSql: " + mysql.getTableForSql());
        System.out.println();
    }

    /**
     * 测试 TileSliceParameter 的 Base32 序列化 / 反序列化
     */
    private static void testTileSliceParameter() {
        String inputUrl = "#jdbc:postgresql://postgres#tcsd1234/192.168.0.110:5432/jwyt_v3/flowable";
        String tableName = "t_" + IdUtil.getSnowflakeNextId();
        String outputUrl = "#jdbc:postgresql://postgres#tcsd1234/192.168.0.110:5432/jwyt_v3/flowable/" + tableName;

        TileSliceParameter parameter = new TileSliceParameter()
                .setLayerName("road_layer")
                .setEdition("v1")
                .setGeomFieldName("geom")
                .setIdFieldName("link_id")
                .setReadStrategy(ReadStrategy.ID_PAGE)
                .setSourceDataSrid(4326)
                .setOutGridSrid(3857)
                .setMinZoom(6)
                .setMaxZoom(14)
                .setDropDensestAsNeeded(true)
                .setCoalesceDensestAsNeeded(true)
                .setQueryStatement("SELECT * FROM flowable.\"amap_traffic_conditions\"")
                .setInputSource(DataSourceConfig.fromProtocolUrlStr(inputUrl))
                .setOutputSource(DataSourceConfig.fromProtocolUrlStr(outputUrl));

        // 序列化
        String encoded = parameter.toBase32();
        System.out.println("[序列化]");
        System.out.println("  Base32 编码: " + encoded);
        System.out.println();

        // 反序列化
        TileSliceParameter decoded = TileSliceParameter.fromBase32(encoded);

        System.out.println("[反序列化验证]");
        System.out.println("  layerName:      " + decoded.getLayerName());
        System.out.println("  edition:        " + decoded.getEdition());
        System.out.println("  geomFieldName:  " + decoded.getGeomFieldName());
        System.out.println("  maxZoom:        " + decoded.getMaxZoom());
        System.out.println("  sourceDataSrid: " + decoded.getSourceDataSrid());
        System.out.println("  outGridSrid:    " + decoded.getOutGridSrid());
        System.out.println();

        // 验证输入数据源
        DataSourceConfig input = decoded.getInputSource();
        System.out.println("[输入数据源]");
        System.out.println("  jdbcUrl:  " + input.getJdbcUrl());
        System.out.println("  username: " + input.getUsername());
        System.out.println("  host:     " + input.getHost());
        System.out.println("  database: " + input.getDatabase());
        System.out.println("  schema:   " + input.getSchemaName());
        System.out.println();

        // 验证输出数据源
        DataSourceConfig output = decoded.getOutputSource();
        System.out.println("[输出数据源]");
        System.out.println("  jdbcUrl:     " + output.getJdbcUrl());
        System.out.println("  username:    " + output.getUsername());
        System.out.println("  tableName:   " + output.getTableName());
        System.out.println("  tableForSql: " + output.getTableNameForSql());
        System.out.println("  host:        " + output.getHost());
        System.out.println("  database:    " + output.getDatabase());
        System.out.println("  schema:      " + output.getSchemaName());
        System.out.println();
    }

    /**
     * 测试多数据库协议格式
     */
    private static void testMultiDatabaseProtocol() {
        String[] urls = {
                "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/mydb/public/tile_cache",
                "#jdbc:postgresql://postgres#secret/10.0.0.1:5432/mydb/public",
                "#jdbc:mysql://root#pass123/192.168.1.100:3306/gisdb//tile_cache",
                "#jdbc:mysql://root#pass123/192.168.1.100:3306/gisdb",
                "#jdbc:sqlserver://sa#pass123/10.0.0.1:1433/mydb/dbo/tile_cache",
                "#jdbc:postgis://admin#geo123/10.0.0.1:5432/geodb/gis/tile_cache",
        };

        for (String url : urls) {
            try {
                ProtocolUrl parsed = new ProtocolUrl(url);
                System.out.printf("  %-75s → jdbcUrl=%s, table=%s, tableForSql=%s%n",
                        url, parsed.toJdbcUrl(), parsed.getTableName(), parsed.getTableForSql());
            } catch (Exception e) {
                System.out.printf("  %-75s → 解析失败: %s%n", url, e.getMessage());
            }
        }
    }
}
