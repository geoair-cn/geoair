package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerGeometryMode;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import vector_tile.VectorTile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** V3 多图层编码器的格式验证。 */
public class MultiLayerMvtEncoderV3Test {

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    public void shouldEncodeTwoInternalLayersIntoOnePbf() throws Exception {
        MvtLayerSliceParameter roads = layer("roads", MvtLayerGeometryMode.ORIGINAL);
        MvtLayerSliceParameter labels = layer("road_labels", MvtLayerGeometryMode.CENTROID);
        MultiLayerTileSliceParameter task = new MultiLayerTileSliceParameter()
                .setOutGridSrid(3857)
                .setTileSetName("base-map")
                .setTileSizeLimitEnabled(false)
                .setLayers(Arrays.asList(roads, labels));

        GirAdvOneRow road = row("road-1", "Road A", geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(-1000000, -1000000), new Coordinate(1000000, 1000000)}));
        GirAdvOneRow label = row("label-1", "Road A", geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(-500000, 0), new Coordinate(500000, 0)}));

        V3TileFeatureGroup group = new V3TileFeatureGroup();
        group.setFeaturesByLayer(new java.util.LinkedHashMap<String, List<GirAdvOneRow>>());
        group.getFeaturesByLayer().put("roads", Arrays.asList(road));
        group.getFeaturesByLayer().put("road_labels", Arrays.asList(label));
        String tileId = GirGeoTools.defaultInstance().getTileGridBingMapOpt().xyzToQuadKey(1, 1, 2);

        PbfInfo pbf = MultiLayerMvtEncoderV3.encode(tileId, group, task);
        VectorTile.Tile tile = VectorTile.Tile.parseFrom(unGzip(pbf.getData()));

        Assert.assertEquals(2, tile.getLayersCount());
        LinkedHashSet<String> layerNames = new LinkedHashSet<>();
        for (VectorTile.Tile.Layer layer : tile.getLayersList()) {
            layerNames.add(layer.getName());
            Assert.assertEquals(1, layer.getFeaturesCount());
        }
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("roads", "road_labels")), layerNames);
        Assert.assertEquals(2, pbf.getZoom());
        Assert.assertEquals(3857, pbf.getGridSrid());
    }

    @Test
    public void shouldDropLowPriorityFeaturesWhenTotalTileSizeIsLimited() throws Exception {
        MvtLayerSliceParameter important = layer("important", MvtLayerGeometryMode.ORIGINAL).setPriority(10);
        MvtLayerSliceParameter optional = layer("optional", MvtLayerGeometryMode.ORIGINAL).setPriority(0);
        MultiLayerTileSliceParameter task = new MultiLayerTileSliceParameter()
                .setOutGridSrid(3857)
                .setTileSetName("base-map")
                .setTileSizeLimitEnabled(true)
                .setTileSizeLimit("1KB")
                .setLayers(Arrays.asList(important, optional));

        V3TileFeatureGroup group = new V3TileFeatureGroup();
        Map<String, List<GirAdvOneRow>> rows = new java.util.LinkedHashMap<>();
        rows.put("important", Arrays.asList(row("main", "main", geometryFactory.createPoint(new Coordinate(0, 0)))));
        GirAdvOneRow[] optionalRows = new GirAdvOneRow[32];
        for (int i = 0; i < optionalRows.length; i++) {
            optionalRows[i] = row("optional-" + i, repeat('x', 180),
                    geometryFactory.createPoint(new Coordinate(i * 1000D, i * 1000D)));
        }
        rows.put("optional", Arrays.asList(optionalRows));
        group.setFeaturesByLayer(rows);

        String tileId = GirGeoTools.defaultInstance().getTileGridBingMapOpt().xyzToQuadKey(1, 1, 2);
        PbfInfo pbf = MultiLayerMvtEncoderV3.encode(tileId, group, task);
        VectorTile.Tile tile = VectorTile.Tile.parseFrom(unGzip(pbf.getData()));

        Assert.assertTrue("最终 gzip PBF 应满足任务总大小限制", pbf.getDataLength() <= 1024);
        Assert.assertEquals("高优先级图层必须保留", "important", tile.getLayers(0).getName());
    }

    private MvtLayerSliceParameter layer(String name, MvtLayerGeometryMode mode) {
        return new MvtLayerSliceParameter()
                .setLayerName(name)
                .setGeomFieldName("geom")
                .setIdFieldName("id")
                .setGeometryMode(mode);
    }

    private GirAdvOneRow row(String id, String name, Object geometry) {
        java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("id", id);
        values.put("name", name);
        values.put("geom", geometry);
        return GirAdvOneRow.ofByMap(values);
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static byte[] unGzip(byte[] compressed) throws IOException {
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[512];
            int length;
            while ((length = input.read(buffer)) >= 0) {
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        }
    }
}
