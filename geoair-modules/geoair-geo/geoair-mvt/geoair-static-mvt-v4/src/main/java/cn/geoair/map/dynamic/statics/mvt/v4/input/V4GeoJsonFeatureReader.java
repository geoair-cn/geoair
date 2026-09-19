package cn.geoair.map.dynamic.statics.mvt.v4.input;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonMode;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

/**
 * V4 GeoJSON 要素读取器：顺序读取标准 FeatureCollection / 单个 Feature / Feature 数组 / GeoJSON Lines。
 *
 * <p>与 V3 的差别：V3 把解析挂在 Spark 的 {@code binaryFiles} / {@code textFile} 上（并行按文件块切分），
 * V4 是单机顺序读，因此这里自己做流式解析 —— <b>内存里只保留当前一个 Feature</b>，
 * 与 V3 文档模式的流式特性一致。</p>
 *
 * <p>路径支持 {@code file:///} 前缀（管理端存的就是这种形式）与本地绝对/相对路径。
 * {@code .gz} 在两种模式下都能解压（V3 的 Lines 模式依赖 Hadoop 编解码器，V4 不依赖）。</p>
 *
 * @author 张逢吉
 */
public final class V4GeoJsonFeatureReader implements V4FeatureReader {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    @Override
    public void read(MvtLayerSliceParameter layer, V4RowConsumer consumer) throws Exception {
        V3GeoJsonInputConfig config = layer.getInputConfig().getGeoJson();
        V3GeoJsonMode mode = resolveMode(config);
        for (String rawPath : config.getPaths()) {
            Path path = normalize(rawPath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("GeoJSON 文件不存在: " + path);
            }
            if (mode == V3GeoJsonMode.GEOJSON_LINES) {
                readLines(path, layer, config, consumer);
            } else {
                readDocument(path, layer, config, consumer);
            }
        }
    }

    /** 与 V3 相同的模式判定：AUTO 时按扩展名决定，任一路径不是 Lines 后缀就按文档模式处理。 */
    private static V3GeoJsonMode resolveMode(V3GeoJsonInputConfig config) {
        if (config.getMode() != null && config.getMode() != V3GeoJsonMode.AUTO) {
            return config.getMode();
        }
        List<String> paths = config.getPaths();
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("GeoJSON 图层必须配置至少一个路径");
        }
        for (String path : paths) {
            String lower = path.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".geojsonl") || lower.endsWith(".ndjson") || lower.endsWith(".jsonl")
                    || lower.endsWith(".geojsonl.gz") || lower.endsWith(".ndjson.gz")
                    || lower.endsWith(".jsonl.gz"))) {
                return V3GeoJsonMode.FEATURE_COLLECTION;
            }
        }
        return V3GeoJsonMode.GEOJSON_LINES;
    }

    /** 去掉 {@code file://} / {@code file:///} 前缀，得到本地文件系统路径。 */
    private static Path normalize(String rawPath) {
        String value = rawPath == null ? "" : rawPath.trim();
        if (value.regionMatches(true, 0, "file:///", 0, 8)) {
            value = value.substring(8);
        } else if (value.regionMatches(true, 0, "file://", 0, 7)) {
            value = value.substring(7);
        }
        return Paths.get(value);
    }

    /** GeoJSON Lines：一行一个 Feature，逐行解析。 */
    private void readLines(Path path, MvtLayerSliceParameter layer,
            V3GeoJsonInputConfig config, V4RowConsumer consumer) throws Exception {
        try (InputStream input = open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            long lineNo = 0L;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String json = normalizeLine(line);
                if (json.isEmpty()) {
                    continue;
                }
                try {
                    JsonNode feature = new ObjectMapper().readTree(json);
                    consumer.accept(V4GeoJsonFeatureMapper.toRow(
                            feature, layer.getGeomFieldName(), layer.getIdFieldName(),
                            layer.resolveSourceDataSrid()));
                } catch (Exception e) {
                    if (!config.isSkipInvalidFeature()) {
                        throw new IllegalArgumentException(
                                "解析 GeoJSON Lines 第 " + lineNo + " 行失败: " + abbreviate(json), e);
                    }
                }
            }
        }
    }

    /** 标准文档：FeatureCollection / 单个 Feature / Feature 数组，流式读。 */
    private void readDocument(Path path, MvtLayerSliceParameter layer,
            V3GeoJsonInputConfig config, V4RowConsumer consumer) throws Exception {
        try (InputStream input = open(path)) {
            JsonParser parser = JSON_FACTORY.createParser(input);
            try {
                DocumentWalker walker = new DocumentWalker(path.toString(), parser, layer, config);
                while (walker.hasNext()) {
                    consumer.accept(walker.next());
                }
            } finally {
                parser.close();
            }
        }
    }

    /** 按扩展名决定是否解压；与 V3 的 {@code .gz} 判定一致。 */
    private static InputStream open(Path path) throws IOException {
        InputStream input = new FileInputStream(path.toFile());
        if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gz")) {
            return new GZIPInputStream(input);
        }
        return input;
    }

    /** 去掉 JSON Sequence 的行首记录分隔符（RS, 0x1E），与 V3 的处理一致。 */
    private static String normalizeLine(String line) {
        if (line == null) {
            return "";
        }
        String value = line.trim();
        return !value.isEmpty() && value.charAt(0) == 0x1E ? value.substring(1).trim() : value;
    }

    private static String abbreviate(String value) {
        return value.length() <= 200 ? value : value.substring(0, 200) + "...";
    }

    /** 流式遍历一个 GeoJSON 文档里的 Feature。 */
    private static final class DocumentWalker {

        private final String source;
        private final JsonParser parser;
        private final ObjectMapper mapper = new ObjectMapper();
        private final MvtLayerSliceParameter layer;
        private final boolean skipInvalidFeature;

        private boolean inFeatureArray;
        private boolean finished;
        private JsonNode singleFeature;
        private GirAdvOneRow next;

        private DocumentWalker(String source, JsonParser parser,
                MvtLayerSliceParameter layer, V3GeoJsonInputConfig config) throws IOException {
            this.source = source;
            this.parser = parser;
            this.layer = layer;
            this.skipInvalidFeature = config.isSkipInvalidFeature();
            initialize();
        }

        private void initialize() throws IOException {
            JsonToken token = parser.nextToken();
            if (token == JsonToken.START_ARRAY) {
                inFeatureArray = true;
                return;
            }
            if (token != JsonToken.START_OBJECT) {
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
            throw new IllegalArgumentException("GeoJSON 文档不包含 features 数组或单个 Feature: " + source);
        }

        private boolean hasNext() {
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
                        return false;
                    }
                    try {
                        next = V4GeoJsonFeatureMapper.toRow(feature, layer.getGeomFieldName(),
                                layer.getIdFieldName(), layer.resolveSourceDataSrid());
                        return true;
                    } catch (Exception e) {
                        if (!skipInvalidFeature) {
                            throw new IllegalArgumentException("解析 GeoJSON Feature 失败，文件: " + source, e);
                        }
                    }
                }
                return false;
            } catch (IOException e) {
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

        private GirAdvOneRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            GirAdvOneRow result = next;
            next = null;
            return result;
        }
    }
}
