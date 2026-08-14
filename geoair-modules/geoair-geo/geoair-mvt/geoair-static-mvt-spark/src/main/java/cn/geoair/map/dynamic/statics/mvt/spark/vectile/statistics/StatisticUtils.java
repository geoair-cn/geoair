package cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.mvt.tools.model.VecConstant;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json.*;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.StaticLog;
import com.alibaba.fastjson2.JSON;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;


public class StatisticUtils {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private static final int MAX_VALUE_COUNT_PER_FIELD = 100;

    private static final String LOG_PREFIX = "[瓦片要素统计]";

    private static final int DEFAULT_SHUFFLE_PARTITIONS = 800;

    public static void statAndWriteJson(
            JavaRDD<Tuple2<String, PbfInfo>> infoRdd,
            String geometryType,
            String staticTableName,
            TileSliceParameter parameter,
            Long totalFeatureCount,
            SparkSession sparkSession) {
        try {

            // 根据FeatureRowID进行去重
            JavaPairRDD<String, Tuple2<String, GirAdvOneRow>> featureIdRdd =
                    infoRdd.flatMapToPair(
                                    tuple -> {
                                        String tileKey = tuple._1;
                                        PbfInfo pbfInfo = tuple._2;
                                        List<GirAdvOneRow> featureList =
                                                pbfInfo.getThisPbfFeatureList() == null
                                                        ? new ArrayList<>()
                                                        : pbfInfo.getThisPbfFeatureList();

                                        pbfInfo.setThisPbfFeatureList(null);
                                        pbfInfo.setData(new byte[0]);
                                        pbfInfo.setDataBoundary(new byte[0]);
                                        pbfInfo.setDataLabel(new byte[0]);
                                        List<Tuple2<String, Tuple2<String, GirAdvOneRow>>> result =
                                                new ArrayList<>();
                                        for (GirAdvOneRow row : featureList) {
                                            // 严格过滤无效数据
                                            if (row == null) continue;
                                            Object featureRowIdObj =
                                                    row.get(VecConstant.FeatureRowID);
                                            if (featureRowIdObj == null) continue;
                                            String featureRowId = featureRowIdObj.toString();
                                            result.add(
                                                    new Tuple2<>(
                                                            featureRowId,
                                                            new Tuple2<>(tileKey, row)));
                                        }
                                        featureList.clear();
                                        return result.iterator();
                                    })
                            .reduceByKey((t1, t2) -> t1);

            JavaPairRDD<String, GirAdvOneRow> tileKeyRdd =
                    featureIdRdd
                            .mapToPair(
                                    tuple -> {
                                        String tileKey = tuple._2._1;
                                        GirAdvOneRow row = tuple._2._2;
                                        return new Tuple2<>(tileKey, row);
                                    })
                            .repartition(DEFAULT_SHUFFLE_PARTITIONS);

            // 3. 直接按原始TileKey聚合
            JavaPairRDD<String, List<GirAdvOneRow>> tileRdd =
                    tileKeyRdd.combineByKey(
                            row -> new ArrayList<>(Collections.singletonList(row)),
                            (list, row) -> {
                                list.add(row);
                                return list;
                            },
                            (list1, list2) -> {
                                list1.addAll(list2);
                                return list1;
                            });

            JavaRDD<GirAdvOneRow> flatRDD = tileRdd.flatMap(tuple -> tuple._2.iterator());

            JavaPairRDD<String, Tuple2<Object, Long>> fieldValueRDD =
                    flatRDD
                            // 先做map转换，用完即丢弃原对象
                            .map(
                                    row -> {
                                        // 1. 复制所有需要的字段数据到临时Map（仅保留统计需要的）
                                        Map<String, Object> tempMap = new HashMap<>();
                                        Set<String> fieldNamesByMap = new HashSet<>(row.keySet());
                                        fieldNamesByMap.remove(parameter.getGeomFieldName());
                                        fieldNamesByMap.remove(VecConstant.FeatureRowID);
                                        for (String fieldName : fieldNamesByMap) {
                                            tempMap.put(
                                                    fieldName,
                                                    FieldStatUtils.getFieldValue(row, fieldName));
                                        }
                                        return tempMap;
                                    })
                            .flatMapToPair(
                                    tempMap -> {
                                        List<Tuple2<String, Tuple2<Object, Long>>> result =
                                                new ArrayList<>();
                                        for (Map.Entry<String, Object> entry : tempMap.entrySet()) {
                                            String fieldName = entry.getKey();
                                            Object value = entry.getValue();
                                            Object finalValue = value == null ? "" : value;
                                            result.add(
                                                    new Tuple2<>(
                                                            fieldName,
                                                            new Tuple2<>(finalValue, 1L)));
                                        }
                                        tempMap.clear();
                                        return result.iterator();
                                    });

            JavaPairRDD<String, Map<Object, Long>> fieldValueCountRDD =
                    fieldValueRDD.aggregateByKey(
                            new HashMap<>(),
                            (map, tuple) -> {
                                if (map.size() >= MAX_VALUE_COUNT_PER_FIELD) {
                                    return map;
                                }
                                Object value = tuple._1;

                                map.put(value, map.getOrDefault(value, 0L) + tuple._2);
                                if (map.size() == MAX_VALUE_COUNT_PER_FIELD) {
                                    System.out.printf(
                                            "%s [内存优化] 字段值数量达到阈值%d，停止统计新值 %n",
                                            LOG_PREFIX, MAX_VALUE_COUNT_PER_FIELD);
                                }
                                return map;
                            },
                            (map1, map2) -> {
                                int remainingCapacity = MAX_VALUE_COUNT_PER_FIELD - map1.size();
                                if (remainingCapacity <= 0) {
                                    return map1;
                                }
                                Map<Object, Long> mergedMap = new HashMap<>(map1);
                                int count = 0;
                                for (Map.Entry<Object, Long> entry : map2.entrySet()) {
                                    if (count >= remainingCapacity) {
                                        break;
                                    }
                                    Object key = entry.getKey();
                                    if (!mergedMap.containsKey(key)) {
                                        mergedMap.put(key, entry.getValue());
                                        count++;
                                    } else {
                                        mergedMap.put(key, mergedMap.get(key) + entry.getValue());
                                    }
                                }
                                return mergedMap;
                            });

            JavaRDD<AttributeStat> attrStatRDD =
                    fieldValueCountRDD.map(
                            tuple -> {
                                String fieldName = tuple._1;
                                Map<Object, Long> valueCount = tuple._2;

                                AttributeStat attrStat = new AttributeStat();
                                attrStat.setAttribute(fieldName);
                                long total = 0L;
                                for (Long cnt : valueCount.values()) {
                                    total += cnt;
                                }
                                attrStat.setCount(total);

                                List<Object> values = new ArrayList<>(valueCount.keySet());
                                attrStat.setValues(values);

                                List<Long> statics = new ArrayList<>(valueCount.values());
                                attrStat.setStatics(statics);

                                if (!valueCount.isEmpty()) {
                                    Object firstValue = values.get(0);
                                    String fieldType = FieldStatUtils.getFieldType(firstValue);
                                    attrStat.setType(fieldType);

                                    if ("Number".equals(fieldType)) {
                                        try {
                                            double min = Double.MAX_VALUE;
                                            double max = Double.MIN_VALUE;
                                            boolean hasNumber = false;

                                            // 手动遍历，避免stream创建额外对象
                                            for (Object v : values) {
                                                if (v instanceof Number) {
                                                    double num = ((Number) v).doubleValue();
                                                    min = Math.min(min, num);
                                                    max = Math.max(max, num);
                                                    hasNumber = true;
                                                }
                                            }
                                            attrStat.setMin(hasNumber ? min : null);
                                            attrStat.setMax(hasNumber ? max : null);
                                        } catch (Exception e) {
                                            attrStat.setType("String");
                                            attrStat.setMin(null);
                                            attrStat.setMax(null);
                                            System.err.printf(
                                                    "%s [字段类型处理] 字段[%s]值类型转换失败：%s %n",
                                                    LOG_PREFIX, fieldName, e.getMessage());
                                        }
                                    } else {
                                        attrStat.setMin(null);
                                        attrStat.setMax(null);
                                    }
                                } else {
                                    attrStat.setType("String");
                                    attrStat.setMin(null);
                                    attrStat.setMax(null);
                                }
                                return attrStat;
                            });
            List<AttributeStat> attributeStats = attrStatRDD.collect();
            // 10. 构建字段名列表（减少stream遍历）
            List<String> fieldNames = new ArrayList<>();
            for (AttributeStat stat : attributeStats) {
                fieldNames.add(stat.getAttribute());
            }

            // 11. 构建主图层JSON并写入
            TileStatRoot root =
                    buildTileStatRoot(
                            parameter.getLayerName(),
                            geometryType,
                            totalFeatureCount,
                            fieldNames,
                            attributeStats,
                            parameter);
            writeJsonToDB(
                    root,
                    staticTableName,
                    parameter.getLayerName(),
                    parameter.getEdition(),
                    sparkSession,
                    parameter,
                    3);

            // 12. 标签图层（如有）
            if (parameter.isCreateLabel()) {
                TileStatRoot rootLabel =
                        buildTileStatRoot(
                                parameter.getLayerNameLabel(),
                                "Point",
                                totalFeatureCount,
                                fieldNames,
                                attributeStats,
                                parameter);
                writeJsonToDB(
                        rootLabel,
                        staticTableName,
                        parameter.getLayerNameLabel(),
                        parameter.getEdition(),
                        sparkSession,
                        parameter,
                        3);
            } else {
                System.out.printf("%s [阶段12/13] 无需创建标签图层，跳过 %n", LOG_PREFIX);
            }

            // 13. 边界图层（如有）
            if (parameter.isCreateBoundary()) {
                TileStatRoot rootBoundary =
                        buildTileStatRoot(
                                parameter.getLayerNameBoundary(),
                                "LineString",
                                totalFeatureCount,
                                fieldNames,
                                attributeStats,
                                parameter);
                writeJsonToDB(
                        rootBoundary,
                        staticTableName,
                        parameter.getLayerNameBoundary(),
                        parameter.getEdition(),
                        sparkSession,
                        parameter,
                        3);
            } else {
                System.out.printf("%s [阶段13/13] 无需创建边界图层，跳过 %n", LOG_PREFIX);
            }

        } catch (Exception e) {
            log.error("瓦片要素统计失败", e);
            throw new RuntimeException("瓦片要素统计失败", e);
        }
    }

    /**
     * 构建顶层JSON结构（简化逻辑，减少临时对象）
     */
    private static TileStatRoot buildTileStatRoot(
            String layerId,
            String geometryType,
            long totalFeatureCount,
            List<String> fieldNames,
            List<AttributeStat> attributeStats,
            TileSliceParameter parameter) {
        long buildStartTime = System.currentTimeMillis();
        System.out.printf("%s [构建JSON] 开始构建图层[%s]的统计JSON结构 %n", LOG_PREFIX, layerId);

        // 构建矢量图层元数据
        VectorLayer vectorLayer = new VectorLayer();
        vectorLayer.setId(layerId);

        // 手动构建fields Map，减少stream开销
        Map<String, String> fields = new HashMap<>(attributeStats.size());
        for (AttributeStat attr : attributeStats) {
            String type =
                    attr.getType() == null
                            ? "String"
                            : attr.getType().equalsIgnoreCase("number")
                            ? "Number"
                            : attr.getType().equalsIgnoreCase("boolean")
                            ? "Boolean"
                            : "String";
            fields.put(attr.getAttribute(), type);
        }

        vectorLayer.setFields(fields);
        vectorLayer.setMaxzoom(parameter.getMaxZoom());
        vectorLayer.setMinzoom(parameter.getMinZoom());
        vectorLayer.setDescription("by spark auto static - " + DateUtil.now());

        // 构建瓦片统计总览
        LayerStat layerStat = new LayerStat();
        layerStat.setLayer(layerId);
        layerStat.setCount(totalFeatureCount);
        layerStat.setGeometry(geometryType);
        layerStat.setAttributeCount(fieldNames.size());
        layerStat.setAttributes(attributeStats);

        TileStats tileStats = new TileStats();
        tileStats.setLayerCount(1);
        tileStats.setLayers(Collections.singletonList(layerStat));

        TileStatRoot root = new TileStatRoot();
        root.setVector_layers(Collections.singletonList(vectorLayer));
        root.setTilestats(tileStats);

        long buildEndTime = System.currentTimeMillis();
        System.out.printf(
                "%s [构建JSON] 图层[%s]的统计JSON结构构建完成，耗时=%dms，JSON长度=%d %n",
                LOG_PREFIX,
                layerId,
                buildEndTime - buildStartTime,
                JSON.toJSONString(root).length());
        return root;
    }

    /**
     * 写入JSON到数据库（带重试机制）
     */
    private static void writeJsonToDB(
            TileStatRoot root,
            String staticTableName,
            String layerName,
            String version,
            SparkSession sparkSession,
            TileSliceParameter parameter,
            int retryTimes) {
        String jsonStr = JSON.toJSONString(root);
        System.out.printf(
                "%s [写入数据库] 准备写入图层[%s]到表[%s]，JSON长度=%d %n",
                LOG_PREFIX, layerName, staticTableName, jsonStr.length());

        // 定义DataFrame的Schema
        StructType schema =
                DataTypes.createStructType(
                        Arrays.asList(
                                DataTypes.createStructField("track_id", DataTypes.StringType, true),
                                DataTypes.createStructField(
                                        "layer_name", DataTypes.StringType, true),
                                DataTypes.createStructField("version", DataTypes.StringType, true),
                                DataTypes.createStructField(
                                        "time_create", DataTypes.StringType, true),
                                DataTypes.createStructField("timestamp", DataTypes.LongType, true),
                                DataTypes.createStructField(
                                        "json_str", DataTypes.StringType, true)));

        StaticLog.info(
                "TrackId:{},layerName:{},version:{}",
                parameter.getTrackId(),
                parameter.getLayerName(),
                version);

        // 构建单行DataFrame
        Row row =
                RowFactory.create(
                        parameter.getTrackId(),
                        layerName,
                        version,
                        DateUtil.now(),
                        System.currentTimeMillis(),
                        jsonStr);
        Dataset<Row> df = sparkSession.createDataFrame(Collections.singletonList(row), schema);
        DataSourceConfig outputSource = parameter.getOutputSource();
        // 带重试的写入逻辑
        Exception lastException = null;
        for (int i = 0; i < retryTimes; i++) {
            try {
                long writeStartTime = System.currentTimeMillis();
                System.out.printf(
                        "%s [写入数据库-重试%d] 开始写入图层[%s]到PostgreSQL %n", LOG_PREFIX, i + 1, layerName);

                df.write()
                        .format("jdbc")
                        .option("url", outputSource.getJdbcUrl())
                        .option("dbtable", StrUtil.wrap(staticTableName, "\""))
                        .option("user", outputSource.getUsername())
                        .option("password", outputSource.getPassword())
                        .option("batchsize", "50")
                        .option("rewriteBatchedStatements", "true")
                        .mode("append")
                        .save();

                long writeEndTime = System.currentTimeMillis();
                System.out.printf(
                        "%s [写入数据库-重试%d] 图层[%s]写入成功，耗时=%dms %n",
                        LOG_PREFIX, i + 1, layerName, writeEndTime - writeStartTime);
                return;
            } catch (Exception e) {
                lastException = e;
                long sleepTime = TimeUnit.SECONDS.toMillis(i + 1);
                System.err.printf(
                        "%s [写入数据库-重试%d] 图层[%s]写入失败：%s，休眠%dms后重试...%n",
                        LOG_PREFIX, i + 1, layerName, e.getMessage(), sleepTime);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.printf("%s [写入数据库-重试%d] 休眠被中断，停止重试 %n", LOG_PREFIX, i + 1);
                    break;
                }
            }
        }

        // 所有重试失败，抛出异常
        String errorMsg =
                String.format(
                        "%s 写入数据库失败（重试%d次），表名：%s，layer：%s",
                        LOG_PREFIX, retryTimes, staticTableName, layerName);
        System.err.println(errorMsg);
        throw new RuntimeException(errorMsg, lastException);
    }
}
