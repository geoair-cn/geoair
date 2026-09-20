package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/** V3 输入配置的任务传输序列化验证。 */
public class V3LayerInputConfigTest {

    @Test
    public void shouldRoundTripJdbcAndGeoJsonInputsThroughBase32() {
        MvtLayerSliceParameter jdbcLayer = new MvtLayerSliceParameter()
                .setLayerName("road")
                .setGeomFieldName("geom")
                .setSourceDataSrid(4326)
                .setInputConfig(V3LayerInputConfig.jdbc(new V3JdbcInputConfig()
                        .setDataSource(DataSourceConfig.of("jdbc:postgresql://localhost/gis", "postgres", "secret"))
                        .setQueryStatement("select id, geom from road")
                        .setReadStrategy(ReadStrategy.ID_PAGE)
                        .setMaxPartitionNum(8)));
        MvtLayerSliceParameter geoJsonLayer = new MvtLayerSliceParameter()
                .setLayerName("boundary")
                .setInputConfig(V3LayerInputConfig.geoJson(new V3GeoJsonInputConfig()
                        .setPaths(Arrays.asList("hdfs:///data/a.geojson", "hdfs:///data/b.geojson"))
                        .setMode(V3GeoJsonMode.FEATURE_COLLECTION)
                        .setMinPartitionNum(4)
                        .setSkipInvalidFeature(true)));
        MultiLayerTileSliceParameter source = new MultiLayerTileSliceParameter()
                .setTileSetName("base_map")
                .setOutputConfig(V3TileOutputConfig.localDirectory("/tmp/base-map"))
                .setLayers(Arrays.asList(jdbcLayer, geoJsonLayer));

        MultiLayerTileSliceParameter decoded = MultiLayerTileSliceParameter.fromBase32(source.toBase32());

        Assert.assertEquals(V3TileInputType.JDBC,
                decoded.getLayers().get(0).getInputConfig().getInputType());
        Assert.assertEquals("select id, geom from road",
                decoded.getLayers().get(0).getInputConfig().getJdbc().getQueryStatement());
        Assert.assertEquals(V3TileInputType.GEOJSON,
                decoded.getLayers().get(1).getInputConfig().getInputType());
        Assert.assertEquals(2,
                decoded.getLayers().get(1).getInputConfig().getGeoJson().getPaths().size());
        Assert.assertTrue(decoded.getLayers().get(1).getInputConfig().getGeoJson().isSkipInvalidFeature());
        Assert.assertEquals(4326, decoded.getLayers().get(1).resolveSourceDataSrid());
    }
}
