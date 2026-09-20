package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.input;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonMode;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.input.PortableDataStream;
import org.apache.spark.sql.SparkSession;
import org.locationtech.jts.geom.Geometry;
import scala.Tuple2;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

/**
 * V3 GeoJSON 要素读取器。
 *
 * <p>标准 FeatureCollection 采用 Jackson 流式解析，一个文件只在内存中保留当前 Feature；
 * GeoJSON Lines 交给 Spark 文本输入按文件块并行读取。两种格式最终都转换为
 * {@link GirAdvOneRow}，因此后续坐标转换和瓦片聚合链路与 JDBC 输入一致。</p>
 *
 * @author 张逢吉
 */
final class V3GeoJsonFeatureReader implements V3FeatureReader {

    private static final int DEFAULT_PARTITIONS = 20;

    @Override
    public JavaRDD<GirAdvOneRow> read(SparkSession sparkSession, MvtLayerSliceParameter layer) {
        V3GeoJsonInputConfig config = layer.getInputConfig().getGeoJson();
        int minPartitions = config.getMinPartitionNum() == null
                ? DEFAULT_PARTITIONS : Math.max(1, config.getMinPartitionNum());
        String pathExpression = joinPaths(config.getPaths());
        JavaSparkContext sparkContext = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
        V3GeoJsonMode mode = resolveMode(config);
        if (mode == V3GeoJsonMode.GEOJSON_LINES) {
            return sparkContext.textFile(pathExpression, minPartitions)
                    .flatMap(new GeoJsonLineFunction(layer));
        }
        JavaPairRDD<String, PortableDataStream> files = sparkContext.binaryFiles(pathExpression, minPartitions);
        return files.flatMap(new GeoJsonDocumentFunction(layer));
    }

    private static V3GeoJsonMode resolveMode(V3GeoJsonInputConfig config) {
        if (config.getMode() != null && config.getMode() != V3GeoJsonMode.AUTO) {
            return config.getMode();
        }
        for (String path : config.getPaths()) {
            String lower = path.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".geojsonl") || lower.endsWith(".ndjson") || lower.endsWith(".jsonl")
                    || lower.endsWith(".geojsonl.gz") || lower.endsWith(".ndjson.gz")
                    || lower.endsWith(".jsonl.gz"))) {
                return V3GeoJsonMode.FEATURE_COLLECTION;
            }
        }
        return V3GeoJsonMode.GEOJSON_LINES;
    }

    private static String joinPaths(List<String> paths) {
        StringBuilder result = new StringBuilder();
        for (String path : paths) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(path.trim());
        }
        return result.toString();
    }

    /** 每行解析一个独立 GeoJSON Feature。 */
    private static final class GeoJsonLineFunction implements FlatMapFunction<String, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final String geomFieldName;
        private final String idFieldName;
        private final int sourceSrid;
        private final boolean skipInvalidFeature;

        private GeoJsonLineFunction(MvtLayerSliceParameter layer) {
            this.geomFieldName = layer.getGeomFieldName();
            this.idFieldName = layer.getIdFieldName();
            this.sourceSrid = layer.resolveSourceDataSrid();
            this.skipInvalidFeature = layer.getInputConfig().getGeoJson().isSkipInvalidFeature();
        }

        @Override
        public Iterator<GirAdvOneRow> call(String line) throws Exception {
            String json = normalizeJsonSequenceLine(line);
            if (json.isEmpty()) {
                return Collections.emptyIterator();
            }
            try {
                JsonNode feature = new ObjectMapper().readTree(json);
                GirAdvOneRow row = V3GeoJsonFeatureMapper.toRow(
                        feature, geomFieldName, idFieldName, sourceSrid);
                return Collections.singletonList(row).iterator();
            } catch (Exception e) {
                if (skipInvalidFeature) {
                    return Collections.emptyIterator();
                }
                throw new IllegalArgumentException("解析 GeoJSON Lines Feature 失败: " + abbreviate(json), e);
            }
        }

        private static String normalizeJsonSequenceLine(String line) {
            if (line == null) {
                return "";
            }
            String value = line.trim();
            return !value.isEmpty() && value.charAt(0) == 0x1E ? value.substring(1).trim() : value;
        }

        private static String abbreviate(String value) {
            return value.length() <= 200 ? value : value.substring(0, 200) + "...";
        }
    }

    /** 每个 Spark 输入记录流式读取一个标准 GeoJSON 文档。 */
    private static final class GeoJsonDocumentFunction
            implements FlatMapFunction<Tuple2<String, PortableDataStream>, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final String geomFieldName;
        private final String idFieldName;
        private final int sourceSrid;
        private final boolean skipInvalidFeature;

        private GeoJsonDocumentFunction(MvtLayerSliceParameter layer) {
            this.geomFieldName = layer.getGeomFieldName();
            this.idFieldName = layer.getIdFieldName();
            this.sourceSrid = layer.resolveSourceDataSrid();
            this.skipInvalidFeature = layer.getInputConfig().getGeoJson().isSkipInvalidFeature();
        }

        @Override
        public Iterator<GirAdvOneRow> call(Tuple2<String, PortableDataStream> file) throws Exception {
            InputStream input = file._2().open();
            if (file._1().toLowerCase(Locale.ROOT).endsWith(".gz")) {
                input = new GZIPInputStream(input);
            }
            return new GeoJsonDocumentIterator(file._1(), input, geomFieldName,
                    idFieldName, sourceSrid, skipInvalidFeature);
        }
    }

    /** 流式读取一个 FeatureCollection，并在消费完成或异常时关闭文件。 */
    static final class GeoJsonDocumentIterator implements Iterator<GirAdvOneRow>, Serializable {
        private static final long serialVersionUID = 1L;

        private final String source;
        private final String geomFieldName;
        private final String idFieldName;
        private final int sourceSrid;
        private final boolean skipInvalidFeature;
        private transient InputStream input;
        private transient JsonParser parser;
        private transient ObjectMapper mapper;
        private transient JsonNode singleFeature;
        private transient GirAdvOneRow next;
        private transient boolean inFeatureArray;
        private transient boolean finished;

        GeoJsonDocumentIterator(String source, InputStream input, String geomFieldName,
                String idFieldName, int sourceSrid, boolean skipInvalidFeature) throws IOException {
            this.source = source;
            this.geomFieldName = geomFieldName;
            this.idFieldName = idFieldName;
            this.sourceSrid = sourceSrid;
            this.skipInvalidFeature = skipInvalidFeature;
            this.input = input;
            this.mapper = new ObjectMapper();
            this.parser = new JsonFactory().createParser(input);
            initializeDocument();
        }

        private void initializeDocument() throws IOException {
            JsonToken token = parser.nextToken();
            if (token == JsonToken.START_ARRAY) {
                inFeatureArray = true;
                return;
            }
            if (token != JsonToken.START_OBJECT) {
                close();
                throw new IllegalArgumentException(
                        "GeoJSON 文档根节点必须是 FeatureCollection、Feature 或 Feature 数组: " + source);
            }
            ObjectNode candidate = mapper.createObjectNode();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) {
                    continue;
                }
                String fieldName = parser.currentName();
                JsonToken valueToken = parser.nextToken();
                if ("features".equals(fieldName) && valueToken == JsonToken.START_ARRAY) {
                    inFeatureArray = true;
                    return;
                }
                if ("type".equals(fieldName) || "id".equals(fieldName)
                        || "geometry".equals(fieldName) || "properties".equals(fieldName)) {
                    candidate.set(fieldName, mapper.readTree(parser));
                } else {
                    parser.skipChildren();
                }
            }
            if ("Feature".equals(candidate.path("type").asText())) {
                singleFeature = candidate;
                return;
            }
            close();
            throw new IllegalArgumentException("GeoJSON 文档不包含 features 数组或单个 Feature: " + source);
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (finished) {
                return false;
            }
            try {
                while (!finished) {
                    JsonNode feature = readNextFeature();
                    if (feature == null) {
                        close();
                        return false;
                    }
                    try {
                        next = V3GeoJsonFeatureMapper.toRow(
                                feature, geomFieldName, idFieldName, sourceSrid);
                        return true;
                    } catch (Exception e) {
                        if (!skipInvalidFeature) {
                            close();
                            throw new IllegalArgumentException("解析 GeoJSON Feature 失败，文件: " + source, e);
                        }
                    }
                }
                return false;
            } catch (IOException e) {
                closeQuietly();
                throw new IllegalStateException("读取 GeoJSON 文件失败: " + source, e);
            }
        }

        private JsonNode readNextFeature() throws IOException {
            if (singleFeature != null) {
                JsonNode result = singleFeature;
                singleFeature = null;
                finished = true;
                return result;
            }
            if (!inFeatureArray) {
                finished = true;
                return null;
            }
            JsonToken token = parser.nextToken();
            if (token == JsonToken.END_ARRAY || token == null) {
                finished = true;
                return null;
            }
            if (token != JsonToken.START_OBJECT) {
                parser.skipChildren();
                if (skipInvalidFeature) {
                    return readNextFeature();
                }
                throw new IllegalArgumentException("GeoJSON features 数组中存在非对象元素: " + source);
            }
            return mapper.readTree(parser);
        }

        @Override
        public GirAdvOneRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            GirAdvOneRow result = next;
            next = null;
            return result;
        }

        private void close() throws IOException {
            finished = true;
            if (parser != null) {
                parser.close();
                parser = null;
            } else if (input != null) {
                input.close();
            }
            input = null;
        }

        private void closeQuietly() {
            try {
                close();
            } catch (IOException ignored) {
                // 原始读取异常优先返回。
            }
        }
    }

    /** 将一个 GeoJSON Feature 映射为切片链路使用的行对象。 */
    static final class V3GeoJsonFeatureMapper {

        private V3GeoJsonFeatureMapper() {
        }

        static GirAdvOneRow toRow(JsonNode feature, String geomFieldName,
                String idFieldName, int sourceSrid) {
            if (feature == null || !feature.isObject()
                    || !"Feature".equals(feature.path("type").asText())) {
                throw new IllegalArgumentException("GeoJSON 节点不是 Feature");
            }
            JsonNode geometryNode = feature.get("geometry");
            if (geometryNode == null || geometryNode.isNull()) {
                throw new IllegalArgumentException("GeoJSON Feature 缺少 geometry");
            }
            Geometry geometry = GirGeoTools.defaultInstance().getFormatOpt()
                    .geojsonToJtsGeometry(geometryNode.toString(), false);
            if (geometry == null || geometry.isEmpty()) {
                throw new IllegalArgumentException("GeoJSON Feature geometry 为空或无法解析");
            }
            geometry.setSRID(sourceSrid);

            Map<String, Object> values = new LinkedHashMap<>();
            JsonNode properties = feature.get("properties");
            if (properties != null && properties.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    values.put(field.getKey(), toAttributeValue(field.getValue()));
                }
            }
            if (idFieldName != null && !idFieldName.trim().isEmpty()
                    && !values.containsKey(idFieldName) && feature.has("id")) {
                values.put(idFieldName, toAttributeValue(feature.get("id")));
            }
            values.put(geomFieldName, geometry);
            return GirAdvOneRow.ofByMap(values);
        }

        private static Object toAttributeValue(JsonNode value) {
            if (value == null || value.isNull()) {
                return "";
            }
            if (value.isTextual()) {
                return value.textValue();
            }
            if (value.isBoolean()) {
                return value.booleanValue();
            }
            if (value.isInt()) {
                return value.intValue();
            }
            if (value.isIntegralNumber()) {
                return value.longValue();
            }
            if (value.isFloatingPointNumber()) {
                return value.doubleValue();
            }
            return value.toString();
        }
    }
}
