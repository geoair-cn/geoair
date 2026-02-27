package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base32;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/29 09:32
 * @description： 基于tippecanoe的瓦片参数设置
 * @note: 对应tippecanoe命令行参数，字段名与参数名保持语义一致
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TileSliceParameter implements Serializable {

    // 获取数据的策略
    private ReadStrategy readStrategy = ReadStrategy.ID_PAGE;


    // ===================== 输入信息配置=====================
    /**
     * 输入连接信息
     */
    public PgConnectInfo inputConnectInfo;
    /**
     * 输出连接信息
     */
    public PgConnectInfo outPutConnectInfo;

    /**
     * 几何字段名称
     */
    private String geomFieldName;

    /**
     * ID字段名称
     */
    private String idFieldName;

    /**
     * 查询语句
     */
    private String queryStatement;


    /**
     * 瓦片图层名称
     * 对应命令行：-l/--layer
     */
    private String layerName;

    /**
     * 版本号
     */
    private String edition;

    /**
     * 数据坐标系（默认EPSG:3857 Web墨卡托）
     * 对应命令行：-s/--srs
     */
    private int sourceDataSrid = 3857;

    // ===================== 输出信息配置=====================

    /**
     * 输出的网格坐标，可选值 3857/4490
     */
    private int outGridSrid = 3857;

    // ===================== 缩放级别配置 =====================
    /**
     * 最小缩放级别
     * 对应命令行：-Z/--minimum-zoom
     */
    private Integer minZoom = 4;

    /**
     * 最大缩放级别
     * 对应命令行：-z/--maximum-zoom
     */
    private Integer maxZoom = 15;

//    /**
//     * 瓦片分辨率（像素精度，默认4，值越小精度越低体积越小）
//     * 对应命令行：-r/--resolution
//     */
//    private Integer resolution = 1;

//    /**
//     * 是否自动扩展缩放级别（当maxZoom下仍有要素丢弃时）
//     * 对应命令行：--extend-zooms-if-still-dropping
//     */
//    private boolean extendZoomsIfStillDropping = true;     // 这个参数需要重启任务，不太好调整，就暂时先不管了

    // ===================== 瓦片限制配置 =====================
    /**
     * 是否开启单瓦片要素数限制
     * 对应命令行：--no-feature-limit
     */
    private boolean enableFeatureLimitIs = false;

    /**
     * 是否开启单瓦片大小限制
     * 对应命令行：--no-tile-size-limit
     */
    private boolean enableFeatureSizeLimit = false;


    // ===================== 要素过滤/优化配置 =====================
    /**
     * 保留的属性字段列表（默认空，保留所有字段）
     * 对应命令行：-y/--include
     */
    private List<String> includeFields = new ArrayList<>();

    /**
     * 是否按密度丢弃要素（优先丢弃高密度区域）
     * 对应命令行：--drop-densest-as-needed
     */
    private boolean dropDensestAsNeeded = true;

    /**
     * 是否按密度合并要素（合并高密度区域相邻要素）
     * 对应命令行：--coalesce-densest-as-needed
     */
    private boolean coalesceDensestAsNeeded = true;


    // ===================== 扩展参数  =====================
    /**
     * 单瓦片最大要素数（默认tippecanoe内置值，仅enableFeatureLimit=false时生效）
     * 对应命令行：-f/--feature-limit
     */
    private Integer featureLimit;

    /**
     * 单瓦片最大字节数  输入100KB，1MB这样可读的字符（默认tippecanoe内置值，仅enableTileSizeLimit=false时生效）
     * 对应命令行：-S/--tile-size
     */
    private String tileSizeLimit = "2MB";

    /**
     * 是否启用要素简化（默认false）
     * 对应命令行：-D/--simplification
     */
    private Integer simplificationLevel;

    /**
     * 要素聚合距离（像素，默认0）
     * 对应命令行：-g/--coalesce
     */
    private Integer coalesceDistance;

    // ===================== 系统参数  =====================

    /**
     * 对应分页获取数据的时候，最大的页数
     */
    private Integer maxPartionNum = 20;

    // ===================== 其他参数  =====================
    /**
     * 是否创建边界
     *
     * @return
     */
    private boolean createBoundary = Boolean.FALSE;

    /**
     * 边界的表名
     */
    private String tableNameBoundary = null;
    /**
     * 边界的图层名称
     */
    private String layerNameBoundary = null;
    /**
     * 是否创建标签
     */
    private boolean createLabel = Boolean.FALSE;

    /**
     * 标签的表名
     */
    private String tableNameLabel = null;

    /**
     * 标签的图层名称
     */
    private String layerNameLabel = null;


    /**
     * 统计属性值
     *
     * @return
     */
    private boolean statisticsIs = Boolean.TRUE;

    /**
     * 统计的json存放的根路径
     */
    private String staticTableName = "static_table_json_def";

    /**
     * 当前切片任务的主图层的图层类型，主要用于生成统计信息的json构建
     *
     * @return
     */
    private AdvEnumsTypeGeom typeGeom;

    /**
     * 当前切片任务中产生的一些系统字段信息
     *
     * @return
     */
    private Set<String> sysIncludeFields = new HashSet<>();

    /**
     * 当前任务的流水号，与系统参数无关
     *
     * @return
     */
    private String trackId = IdUtil.fastSimpleUUID();


    public Long getTileSizeLimitByte() {
        if (tileSizeLimit == null) {
            return null;
        }
        return DataSizeUtil.parse(tileSizeLimit);
    }

    public TileSliceParameter copy() {
        TileSliceParameter copy = new TileSliceParameter();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }

    public static TileSliceParameter fromBase32(String baseString) {
        try {
            // 缓存未命中，执行原逻辑
            String encode = URLUtil.decode(baseString);
            String s = Base32.decodeStr(encode);
            TileSliceParameter params = JSON.parseObject(s, TileSliceParameter.class);
            return params;
        } catch (Exception e) {
            // 处理异常（如解码失败）
            throw new RuntimeException("Failed to parse TileRequestParams from base32: " + baseString, e);
        }
    }

    public String toBase32() {
        // 序列化为JSON时，忽略值为null的字段
        String jsonStr = JSON.toJSONString(this);
        // 移除空值，压缩体积
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        jsonObject.entrySet().removeIf(entry -> ObjectUtil.isEmpty(entry.getValue()));
        // 对处理后的JSON字符串进行Base32编码
        String encode = Base32.encode(jsonObject.toString());
        return encode;
    }
}
