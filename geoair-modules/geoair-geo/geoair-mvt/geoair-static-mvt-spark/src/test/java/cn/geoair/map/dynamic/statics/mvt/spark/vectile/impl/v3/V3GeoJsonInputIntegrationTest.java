package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonMode;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3LayerInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputConfig;
import org.apache.spark.sql.SparkSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import vector_tile.VectorTile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 从两种 GeoJSON 格式读取、聚合并输出多图层 PBF 的端到端验证。 */
public class V3GeoJsonInputIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private SparkSession spark;

    @Before
    public void setUp() {
        spark = SparkSession.builder()
                .appName("v3-geojson-input-test")
                .master("local[2]")
                .config("spark.ui.enabled", "false")
                .config("spark.driver.host", "127.0.0.1")
                .config("spark.sql.shuffle.partitions", "2")
                .getOrCreate();
        spark.sparkContext().setLogLevel("WARN");
    }

    @After
    public void tearDown() {
        if (spark != null) {
            spark.stop();
            SparkSession.clearActiveSession();
            SparkSession.clearDefaultSession();
        }
    }

    @Test
    public void shouldGenerateOnePbfFromFeatureCollectionAndGeoJsonLines() throws Exception {
        Path featureCollection = temporaryFolder.newFile("boundary.geojson").toPath();
        Files.write(featureCollection, ("{\"type\":\"FeatureCollection\",\"features\":["
                + "{\"type\":\"Feature\",\"id\":\"b1\","
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[0,0]},"
                + "\"properties\":{\"name\":\"boundary\"}}]}")
                .getBytes(StandardCharsets.UTF_8));
        Path geoJsonLines = temporaryFolder.newFile("labels.geojsonl").toPath();
        Files.write(geoJsonLines, ("{\"type\":\"Feature\",\"id\":\"l1\","
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[1,1]},"
                + "\"properties\":{\"name\":\"label\"}}\n")
                .getBytes(StandardCharsets.UTF_8));
        Path output = temporaryFolder.newFolder("tiles").toPath();

        MvtLayerSliceParameter boundary = new MvtLayerSliceParameter()
                .setLayerName("boundary")
                .setIdFieldName("id")
                .setMinZoom(0)
                .setMaxZoom(0)
                .setInputConfig(V3LayerInputConfig.geoJson(V3GeoJsonInputConfig.of(
                        featureCollection.toUri().toString())
                        .setMode(V3GeoJsonMode.FEATURE_COLLECTION)
                        .setMinPartitionNum(1)));
        MvtLayerSliceParameter label = new MvtLayerSliceParameter()
                .setLayerName("label")
                .setIdFieldName("id")
                .setMinZoom(0)
                .setMaxZoom(0)
                .setInputConfig(V3LayerInputConfig.geoJson(V3GeoJsonInputConfig.of(
                        geoJsonLines.toUri().toString())
                        .setMode(V3GeoJsonMode.GEOJSON_LINES)
                        .setMinPartitionNum(1)));
        MultiLayerTileSliceParameter task = new MultiLayerTileSliceParameter()
                .setTileSetName("geojson-test")
                .setMinZoom(0)
                .setMaxZoom(0)
                .setOutGridSrid(3857)
                .setReducePartitionNum(1)
                .setTileSizeLimitEnabled(false)
                .setGzipPbf(false)
                .setOutputConfig(V3TileOutputConfig.localDirectory(output.toString())
                        .setWriteMetadata(false))
                .setLayers(Arrays.asList(boundary, label));

        new SparkVectorTileGeneratorV3(spark).doGenerate(task);

        Path tileFile = output.resolve("0/0/0.pbf");
        Assert.assertTrue(Files.isRegularFile(tileFile));
        VectorTile.Tile tile = VectorTile.Tile.parseFrom(Files.readAllBytes(tileFile));
        Set<String> layerNames = new HashSet<>();
        for (VectorTile.Tile.Layer layer : tile.getLayersList()) {
            layerNames.add(layer.getName());
        }
        Assert.assertEquals(new HashSet<>(Arrays.asList("boundary", "label")), layerNames);
    }
}
