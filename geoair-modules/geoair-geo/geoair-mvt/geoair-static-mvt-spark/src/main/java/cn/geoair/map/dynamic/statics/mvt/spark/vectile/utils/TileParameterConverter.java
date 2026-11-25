package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.hutool.core.collection.CollectionUtil;

import java.util.*;

/**
 * TileSliceParameter 与 tippecanoe 命令行参数双向转换工具类 实现： 1. toTippecanoeParams: TileSliceParameter ->
 * 命令行参数字符串 2. fromTippecanoeParams: 命令行参数字符串 -> TileSliceParameter
 */
public class TileParameterConverter {

    // 开关参数映射（DTO字段 -> tippecanoe命令行参数）
    private static final Map<String, String> SWITCH_PARAM_MAPPING = new HashMap<>();

    // 带值参数映射（DTO字段 -> tippecanoe命令行参数）
    private static final Map<String, String> VALUED_PARAM_MAPPING = new HashMap<>();

    // 默认开关参数（必须保留的）
    private static final Set<String> DEFAULT_SWITCH_PARAMS = new HashSet<>();

    // 默认短带值参数（-Z/-z/-r 对应 minZoom/maxZoom/resolution）
    private static final Map<String, Integer> DEFAULT_SHORT_VALUED_PARAMS = new HashMap<>();

    // 命令行参数到DTO字段的反向映射
    private static final Map<String, String> REVERSE_MAPPING = new HashMap<>();

    static {
        // 初始化开关参数映射
        SWITCH_PARAM_MAPPING.put("extendZoomsIfStillDropping", "--extend-zooms-if-still-dropping");
        SWITCH_PARAM_MAPPING.put(
                "featureLimitEnabled", "--feature-limit"); // DTO中false对应--no-feature-limit
        SWITCH_PARAM_MAPPING.put(
                "featureSizeLimitEnabled", "--tile-size-limit"); // DTO中false对应--no-tile-size-limit
        SWITCH_PARAM_MAPPING.put("dropDensestAsNeeded", "--drop-densest-as-needed");
        SWITCH_PARAM_MAPPING.put("coalesceDensestAsNeeded", "--coalesce-densest-as-needed");

        // 初始化带值参数映射
        VALUED_PARAM_MAPPING.put("minZoom", "-Z"); // --minimum-zoom
        VALUED_PARAM_MAPPING.put("maxZoom", "-z"); // --maximum-zoom
        VALUED_PARAM_MAPPING.put("resolution", "-r"); // --resolution
        VALUED_PARAM_MAPPING.put("sourceDataSrid", "-s"); // --srs
        VALUED_PARAM_MAPPING.put("layerName", "-l"); // --layer
        VALUED_PARAM_MAPPING.put("featureLimit", "-f"); // --feature-limit
        VALUED_PARAM_MAPPING.put("tileSizeLimit", "-S"); // --tile-size
        VALUED_PARAM_MAPPING.put("simplificationLevel", "-D"); // --simplification
        VALUED_PARAM_MAPPING.put("coalesceDistance", "--cluster-distance"); // --coalesce

        // 初始化默认开关参数
        DEFAULT_SWITCH_PARAMS.add("--extend-zooms-if-still-dropping");
        DEFAULT_SWITCH_PARAMS.add("--no-feature-limit");
        DEFAULT_SWITCH_PARAMS.add("--no-tile-size-limit");
        DEFAULT_SWITCH_PARAMS.add("--drop-densest-as-needed");
        DEFAULT_SWITCH_PARAMS.add("--coalesce-densest-as-needed");

        // 初始化默认短带值参数
        DEFAULT_SHORT_VALUED_PARAMS.put("-Z", 4); // minZoom默认值
        DEFAULT_SHORT_VALUED_PARAMS.put("-z", 15); // maxZoom默认值
        DEFAULT_SHORT_VALUED_PARAMS.put("-r", 1); // resolution默认值

        // 初始化反向映射（命令行参数 -> DTO字段）
        REVERSE_MAPPING.put("--extend-zooms-if-still-dropping", "extendZoomsIfStillDropping");
        REVERSE_MAPPING.put("--no-feature-limit", "featureLimitEnabled");
        REVERSE_MAPPING.put("--feature-limit", "featureLimitEnabled");
        REVERSE_MAPPING.put("--no-tile-size-limit", "featureSizeLimitEnabled");
        REVERSE_MAPPING.put("--tile-size-limit", "featureSizeLimitEnabled");
        REVERSE_MAPPING.put("--drop-densest-as-needed", "dropDensestAsNeeded");
        REVERSE_MAPPING.put("--coalesce-densest-as-needed", "coalesceDensestAsNeeded");
        REVERSE_MAPPING.put("-Z", "minZoom");
        REVERSE_MAPPING.put("-z", "maxZoom");
        REVERSE_MAPPING.put("-r", "resolution");
        REVERSE_MAPPING.put("-s", "sourceDataSrid");
        REVERSE_MAPPING.put("-l", "layerName");
        REVERSE_MAPPING.put("-f", "featureLimit");
        REVERSE_MAPPING.put("-S", "tileSizeLimit");
        REVERSE_MAPPING.put("-D", "simplificationLevel");
        REVERSE_MAPPING.put("-g", "coalesceDistance");
        REVERSE_MAPPING.put("--cluster-distance", "coalesceDistance");
        REVERSE_MAPPING.put("-y", "includeFields");
    }

    /**
     * 将 TileSliceParameter 转换为 tippecanoe 命令行参数字符串
     *
     * @param parameter 瓦片参数对象
     * @return 命令行参数字符串
     */
    public static String toTippecanoeParams(TileSliceParameter parameter) {
        if (parameter == null) {
            parameter = new TileSliceParameter();
        }

        // 1. 短带值参数（-Z/-z/-r）
        Map<String, Integer> shortValuedParams = new HashMap<>(DEFAULT_SHORT_VALUED_PARAMS);
        if (parameter.getMinZoom() != null) {
            shortValuedParams.put("-Z", parameter.getMinZoom());
        }
        if (parameter.getMaxZoom() != null) {
            shortValuedParams.put("-z", parameter.getMaxZoom());
        }
        // resolution字段在DTO中注释，如需启用可放开
        // if (parameter.getResolution() != null) {
        // shortValuedParams.put("-r", parameter.getResolution());
        // }

        // 2. 开关参数（处理反向逻辑）
        Set<String> switchParams = new HashSet<>(DEFAULT_SWITCH_PARAMS);

        // 要素数限制
        if (parameter.isFeatureLimitEnabled()) {
            switchParams.remove("--no-feature-limit");
            switchParams.add("--feature-limit");
        }

        // 瓦片大小限制
        if (parameter.isFeatureSizeLimitEnabled()) {
            switchParams.remove("--no-tile-size-limit");
            switchParams.add("--tile-size-limit");
        }

        // 丢弃密度要素
        if (!parameter.isDropDensestAsNeeded()) {
            switchParams.remove("--drop-densest-as-needed");
        }

        // 合并密度要素
        if (!parameter.isCoalesceDensestAsNeeded()) {
            switchParams.remove("--coalesce-densest-as-needed");
        }

        // 扩展缩放级别（DTO中注释，如需启用可放开）
        // if (!parameter.isExtendZoomsIfStillDropping()) {
        // switchParams.remove("--extend-zooms-if-still-dropping");
        // }

        // 3. 长带值参数
        Map<String, String> longValuedParams = new HashMap<>();

        // 图层名称
        if (parameter.getLayerName() != null && !parameter.getLayerName().isEmpty()) {
            longValuedParams.put("-l", parameter.getLayerName());
        }

        // 坐标系
        if (parameter.getSourceDataSrid() != 3857) {
            longValuedParams.put("-s", String.valueOf(parameter.getSourceDataSrid()));
        }

        // 要素数限制
        if (parameter.getFeatureLimit() != null) {
            longValuedParams.put("-f", String.valueOf(parameter.getFeatureLimit()));
        }

        // 瓦片大小限制
        if (parameter.getTileSizeLimit() != null) {
            longValuedParams.put("-S", String.valueOf(parameter.getTileSizeLimitByte()));
        }

        // 简化级别
        if (parameter.getSimplificationLevel() != null) {
            longValuedParams.put("-D", String.valueOf(parameter.getSimplificationLevel()));
        }

        // 聚合距离
        if (parameter.getCoalesceDistance() != null) {
            longValuedParams.put("-g", String.valueOf(parameter.getCoalesceDistance()));
        }

        // 包含字段
        if (CollectionUtil.isNotEmpty(parameter.getIncludeFields())) {
            longValuedParams.put("-y", String.join(" ", parameter.getIncludeFields()));
        }

        // 4. 拼接命令行参数
        StringBuilder sb = new StringBuilder();

        // 短参数
        for (String key : Arrays.asList("-Z", "-z", "-r")) {
            sb.append(key).append(shortValuedParams.get(key)).append(" ");
        }

        // 长带值参数
        for (Map.Entry<String, String> entry : longValuedParams.entrySet()) {
            sb.append(entry.getKey()).append(" ").append(entry.getValue()).append(" ");
        }

        // 开关参数
        for (String switchParam : switchParams) {
            sb.append(switchParam).append(" ");
        }

        // 去除末尾空格
        String finalParams = sb.toString().trim();

        // 兜底
        return finalParams.isEmpty() ? getDefaultParams() : finalParams;
    }

    /**
     * 将 tippecanoe 命令行参数解析为 TileSliceParameter
     *
     * @param tippecanoeParams 命令行参数字符串
     * @return 瓦片参数对象
     */
    public static TileSliceParameter fromTippecanoeParams(String tippecanoeParams) {
        TileSliceParameter parameter = new TileSliceParameter();

        if (tippecanoeParams == null || tippecanoeParams.trim().isEmpty()) {
            return parameter; // 返回默认参数
        }

        // 解析命令行参数
        String[] params = tippecanoeParams.trim().split("\\s+");
        int i = 0;

        while (i < params.length) {
            String param = params[i].trim();
            if (param.isEmpty()) {
                i++;
                continue;
            }

            // 处理短带值参数（-Z0/-z11/-r1）
            if (param.matches("^-[Zrz]\\d+$")) {
                String key = param.substring(0, 2);
                String value = param.substring(2);
                parseShortValuedParam(parameter, key, value);
                i++;
            }
            // 处理带值参数（--cluster-distance 44）
            else if (param.startsWith("-") && param.length() > 1) {
                if (REVERSE_MAPPING.containsKey(param)) {
                    String dtoField = REVERSE_MAPPING.get(param);
                    i++;
                    if (i < params.length) {
                        String value = params[i].trim();
                        parseValuedParam(parameter, dtoField, value);
                    }
                } else {
                    i++;
                }
            }
            // 处理开关参数（--extend-zooms-if-still-dropping）
            else if (param.startsWith("--")) {
                parseSwitchParam(parameter, param);
                i++;
            } else {
                i++; // 未知参数，跳过
            }
        }

        return parameter;
    }

    /** 解析短带值参数（-Z/-z/-r） */
    private static void parseShortValuedParam(
            TileSliceParameter parameter, String key, String value) {
        try {
            int intValue = Integer.parseInt(value);
            switch (key) {
                case "-Z":
                    parameter.setMinZoom(intValue);
                    break;
                case "-z":
                    parameter.setMaxZoom(intValue);
                    break;
                case "-r":
                    // parameter.setResolution(intValue);
                    break;
            }
        } catch (NumberFormatException e) {
            // 无效值，跳过
        }
    }

    /** 解析带值参数（如 -f 1000） */
    private static void parseValuedParam(
            TileSliceParameter parameter, String dtoField, String value) {
        try {
            switch (dtoField) {
                case "sourceDataSrid":
                    parameter.setSourceDataSrid(Integer.parseInt(value));
                    break;
                case "featureLimit":
                    parameter.setFeatureLimit(Integer.parseInt(value));
                    break;
                case "tileSizeLimit":
                    parameter.setTileSizeLimit(value);
                    break;
                case "simplificationLevel":
                    parameter.setSimplificationLevel(Integer.parseInt(value));
                    break;
                case "coalesceDistance":
                    parameter.setCoalesceDistance(Integer.parseInt(value));
                    break;
                case "includeFields":
                    // 多个字段用空格分隔
                    parameter.setIncludeFields(Arrays.asList(value.split("\\s+")));
                    break;
                case "layerName":
                    parameter.setLayerName(value);
                    break;
            }
        } catch (NumberFormatException e) {
            // 无效值，跳过
        }
    }

    /** 解析开关参数（如 --no-feature-limit） */
    private static void parseSwitchParam(TileSliceParameter parameter, String param) {
        if (param.equals("--extend-zooms-if-still-dropping")) {
            // parameter.setExtendZoomsIfStillDropping(true);
        } else if (param.equals("--no-feature-limit") || param.equals("--feature-limit")) {
            parameter.setFeatureLimitEnabled(param.equals("--feature-limit"));
        } else if (param.equals("--no-tile-size-limit") || param.equals("--tile-size-limit")) {
            parameter.setFeatureSizeLimitEnabled(param.equals("--tile-size-limit"));
        } else if (param.equals("--drop-densest-as-needed")) {
            parameter.setDropDensestAsNeeded(true);
        } else if (param.equals("--coalesce-densest-as-needed")) {
            parameter.setCoalesceDensestAsNeeded(true);
        }
    }

    /** 获取默认参数字符串 */
    private static String getDefaultParams() {
        StringBuilder sb = new StringBuilder();
        sb.append("-Z4 -z15 -r1 ");
        sb.append(String.join(" ", DEFAULT_SWITCH_PARAMS));
        return sb.toString().trim();
    }
}
