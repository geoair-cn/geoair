package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base32;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 矢量瓦片切片参数。
 * <p>
 * 包含数据读取策略、输入/输出数据源配置、瓦片级别参数、要素优化策略等。
 * 序列化为 JSON 后通过 Base32 编码在 Spark 任务间传递。
 *
 * @author 张逢吉
 * @date 2025/12/29
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TileSliceParameter implements Serializable {

    // ===================== 数据读取策略 =====================

    /** 数据读取策略（默认按 ID 分页） */
    private ReadStrategy readStrategy = ReadStrategy.ID_PAGE;

    // ===================== 数据源配置 =====================

    /**
     * 输入数据源配置（从 PostGIS 读取数据）
     */
    @JSONField(deserializeUsing = DataSourceConfigDeserializer.class)
    private DataSourceConfig inputSource;

    /**
     * 输出数据源配置（写入瓦片缓存表）
     */
    @JSONField(deserializeUsing = DataSourceConfigDeserializer.class)
    private DataSourceConfig outputSource;

    // ===================== 数据字段配置 =====================

    /** 几何字段名称 */
    private String geomFieldName;

    /** ID 字段名称 */
    private String idFieldName;

    /** 查询语句 */
    private String queryStatement;

    /**
     * 瓦片图层名称
     */
    private String layerName;

    /** 版本号 */
    private String edition;

    /**
     * 数据坐标系（默认 EPSG:3857 Web 墨卡托）
     */
    private int sourceDataSrid = 3857;

    // ===================== 输出坐标系配置 =====================

    /** 输出的网格坐标，可选值 3857 / 4490 */
    private int outGridSrid = 3857;

    // ===================== 缩放级别配置 =====================

    /** 最小缩放级别 */
    private Integer minZoom = 4;

    /** 最大缩放级别 */
    private Integer maxZoom = 15;

    // ===================== 瓦片限制配置 =====================

    /** 是否开启单瓦片要素数限制 */
    private boolean featureLimitEnabled = false;

    /** 是否开启单瓦片大小限制 */
    private boolean featureSizeLimitEnabled = false;

    // ===================== 要素过滤 / 优化配置 =====================

    /** 保留的属性字段列表（默认空，保留所有字段） */
    private List<String> includeFields = new ArrayList<>();

    /** 是否按密度丢弃要素（优先丢弃高密度区域） */
    private boolean dropDensestAsNeeded = true;

    /** 是否按密度合并要素（合并高密度区域相邻要素） */
    private boolean coalesceDensestAsNeeded = true;

    // ===================== 扩展参数 =====================

    /** 单瓦片最大要素数（仅 featureLimitEnabled 时生效） */
    private Integer featureLimit;

    /** 单瓦片最大字节数，可读格式如 "100KB"、"2MB"（仅 featureSizeLimitEnabled 时生效） */
    private String tileSizeLimit = "2MB";

    /** 要素简化等级 */
    private Integer simplificationLevel;

    /** 要素聚合距离（像素） */
    private Integer coalesceDistance;

    // ===================== 系统参数 =====================

    /** 分页读取时的最大分区数 */
    private Integer maxPartionNum = 20;

    // ===================== 边界图层配置 =====================

    /** 是否创建边界图层 */
    private boolean createBoundary = Boolean.FALSE;

    /** 边界图层的表名 */
    private String tableNameBoundary = null;

    /** 边界图层的图层名称 */
    private String layerNameBoundary = null;

    // ===================== 标签图层配置 =====================

    /** 是否创建标签图层 */
    private boolean createLabel = Boolean.FALSE;

    /** 标签图层的表名 */
    private String tableNameLabel = null;

    /** 标签图层的图层名称 */
    private String layerNameLabel = null;

    // ===================== 统计配置 =====================

    /** 是否生成属性统计信息 */
    private boolean statisticsEnabled = Boolean.FALSE;

    /** 统计 JSON 存放的表名 */
    private String staticTableName = "static_table_json_def";

    /** 主图层的几何类型，用于生成统计 JSON */
    private AdvEnumsTypeGeom typeGeom;

    /** 系统生成的字段名集合（不参与统计） */
    private Set<String> sysIncludeFields = new HashSet<>();

    /** 当前任务的流水号（与系统参数无关，用于追踪） */
    private String trackId = IdUtil.fastSimpleUUID();

    // ===================== 工具方法 =====================

    /**
     * 解析 tileSizeLimit 为字节数。
     */
    public Long getTileSizeLimitByte() {
        if (tileSizeLimit == null) {
            return null;
        }
        return DataSizeUtil.parse(tileSizeLimit);
    }

    /**
     * 深拷贝当前参数对象。
     */
    public TileSliceParameter copy() {
        TileSliceParameter copy = new TileSliceParameter();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }

    // ===================== 序列化 =====================

    /**
     * 从 Base32 编码的 JSON 字符串反序列化。兼容旧版 PgConnectInfoSimple / PgConnectInfoWithTable 格式。
     */
    public static TileSliceParameter fromBase32(String baseString) {
        try {
            String encoded = URLUtil.decode(baseString);
            String json = Base32.decodeStr(encoded);

            // 移除旧版兼容字段，避免反序列化到错误的类型
            JSONObject jsonObject = JSON.parseObject(json);
            jsonObject.remove("inputConnectInfo");
            jsonObject.remove("outPutConnectInfo");
            jsonObject.remove("inputConnectSimple");
            jsonObject.remove("outPutConnectWithTable");
            // 旧版的 dataSourceGetterFunction 字段不可反序列化，直接移除
            removeNestedField(jsonObject, "inputSource", "dataSourceFactory");
            removeNestedField(jsonObject, "outputSource", "dataSourceFactory");

            TileSliceParameter params = jsonObject.to(TileSliceParameter.class);

            // 兼容旧版字段名：如果新版字段为空，尝试从旧版字段迁移
            migrateLegacyFields(jsonObject, params);

            return params;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse TileSliceParameter from base32: " + baseString, e);
        }
    }

    /**
     * 移除嵌套对象中的指定字段（忽略不存在的情况）。
     */
    private static void removeNestedField(JSONObject root, String parentKey, String childKey) {
        JSONObject parent = root.getJSONObject(parentKey);
        if (parent != null) {
            parent.remove(childKey);
        }
    }

    /**
     * 从旧版 JSON 字段迁移到新版 DataSourceConfig。
     * <p>
     * 旧版使用 inputConnectSimple (PgConnectInfoSimple) 和 outPutConnectWithTable (PgConnectInfoWithTable)，
     * 新版使用 inputSource 和 outputSource (DataSourceConfig)。
     */
    private static void migrateLegacyFields(JSONObject json, TileSliceParameter params) {
        if (params.getInputSource() == null) {
            JSONObject legacy = json.getJSONObject("inputConnectSimple");
            if (legacy != null) {
                params.setInputSource(buildConfigFromLegacy(legacy));
            }
        }
        if (params.getOutputSource() == null) {
            JSONObject legacy = json.getJSONObject("outPutConnectWithTable");
            if (legacy != null) {
                params.setOutputSource(buildConfigFromLegacy(legacy));
            }
        }
    }

    /**
     * 从旧版 PgConnectInfoSimple / PgConnectInfoWithTable 的 JSON 对象构建 DataSourceConfig。
     */
    private static DataSourceConfig buildConfigFromLegacy(JSONObject legacy) {
        DataSourceConfig config = new DataSourceConfig();
        config.setHost(legacy.getString("ip"));
        config.setPort(legacy.getString("port"));
        config.setUsername(legacy.getString("userName"));
        config.setPassword(legacy.getString("passwd"));
        config.setDatabase(legacy.getString("dbName"));
        config.setSchemaName(legacy.getString("schemaName"));
        config.setTableName(legacy.getString("tableName"));

        // 自动构建 JDBC URL
        String host = config.getHost();
        String port = config.getPort() != null ? config.getPort() : "5432";
        String db = config.getDatabase() != null ? config.getDatabase() : "";
        StringBuilder url = new StringBuilder("jdbc:postgresql://")
                .append(host).append(':').append(port).append('/').append(db);
        if (config.getSchemaName() != null) {
            url.append("?currentSchema=").append(config.getSchemaName());
        }
        config.setJdbcUrl(url.toString());

        return config;
    }

    /**
     * 序列化为 Base32 编码的 JSON 字符串（移除 null 值以压缩体积）。
     */
    public String toBase32() {
        String jsonStr = JSON.toJSONString(this);
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        jsonObject.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        String encode = Base32.encode(jsonObject.toString());
        return encode;
    }
}
