package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.input;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3LayerInputConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** V3 GeoJSON 流式解析与输入配置校验。 */
public class V3GeoJsonFeatureReaderTest {

    @Test
    public void shouldStreamFeatureCollectionAndNormalizeAttributes() throws Exception {
        String json = "{\"type\":\"FeatureCollection\",\"features\":["
                + "{\"type\":\"Feature\",\"id\":\"p-1\","
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[120.5,30.25]},"
                + "\"properties\":{\"name\":\"杭州\",\"rank\":3,\"enabled\":true,"
                + "\"tags\":[\"a\",\"b\"]}},"
                + "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\","
                + "\"coordinates\":[121.0,31.0]},\"properties\":{\"name\":\"上海\"}}]}";

        Iterator<GirAdvOneRow> iterator = new V3GeoJsonFeatureReader.GeoJsonDocumentIterator(
                "memory.geojson", new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                "geometry", "id", 4326, false);
        List<GirAdvOneRow> rows = collect(iterator);

        Assert.assertEquals(2, rows.size());
        Assert.assertEquals("p-1", rows.get(0).get("id"));
        Assert.assertEquals("杭州", rows.get(0).get("name"));
        Assert.assertEquals(3, rows.get(0).get("rank"));
        Assert.assertEquals(true, rows.get(0).get("enabled"));
        Assert.assertEquals("[\"a\",\"b\"]", rows.get(0).get("tags"));
        Geometry geometry = (Geometry) rows.get(0).get("geometry");
        Assert.assertEquals(4326, geometry.getSRID());
        Assert.assertEquals(120.5D, geometry.getCoordinate().x, 0D);
    }

    @Test
    public void shouldSkipInvalidFeatureWhenConfigured() throws Exception {
        String json = "{\"type\":\"FeatureCollection\",\"features\":["
                + "{\"type\":\"Feature\",\"geometry\":null,\"properties\":{}},"
                + "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\","
                + "\"coordinates\":[0,0]},\"properties\":{\"name\":\"valid\"}}]}";

        Iterator<GirAdvOneRow> iterator = new V3GeoJsonFeatureReader.GeoJsonDocumentIterator(
                "skip.geojson", new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                "geometry", null, 4326, true);

        List<GirAdvOneRow> rows = collect(iterator);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals("valid", rows.get(0).get("name"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailInvalidFeatureInStrictMode() throws Exception {
        String json = "{\"type\":\"FeatureCollection\",\"features\":["
                + "{\"type\":\"Feature\",\"geometry\":null,\"properties\":{}}]}";
        Iterator<GirAdvOneRow> iterator = new V3GeoJsonFeatureReader.GeoJsonDocumentIterator(
                "strict.geojson", new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                "geometry", null, 4326, false);
        iterator.hasNext();
    }

    @Test
    public void shouldMapSingleFeatureAndUseCustomGeometryField() throws Exception {
        String json = "{\"type\":\"Feature\",\"id\":9,"
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[10,20]},"
                + "\"properties\":{\"nested\":{\"code\":1}}}";
        GirAdvOneRow row = V3GeoJsonFeatureReader.V3GeoJsonFeatureMapper.toRow(
                new ObjectMapper().readTree(json), "geom", "fid", 4490);

        Assert.assertEquals(9, row.get("fid"));
        Assert.assertEquals("{\"code\":1}", row.get("nested"));
        Assert.assertEquals(4490, ((Geometry) row.get("geom")).getSRID());
    }

    @Test
    public void shouldValidateGeoJsonInputAndResolveDefaultSrid() {
        MvtLayerSliceParameter layer = new MvtLayerSliceParameter()
                .setLayerName("boundary")
                .setInputConfig(V3LayerInputConfig.geoJson(V3GeoJsonInputConfig.of("file:///data/boundary.geojson")));

        V3FeatureReaderFactory.validate(layer);
        Assert.assertEquals(4326, layer.resolveSourceDataSrid());
        Assert.assertTrue(V3FeatureReaderFactory.getReader(layer) instanceof V3GeoJsonFeatureReader);
    }

    private static List<GirAdvOneRow> collect(Iterator<GirAdvOneRow> iterator) {
        List<GirAdvOneRow> rows = new ArrayList<>();
        while (iterator.hasNext()) {
            rows.add(iterator.next());
        }
        return rows;
    }
}
