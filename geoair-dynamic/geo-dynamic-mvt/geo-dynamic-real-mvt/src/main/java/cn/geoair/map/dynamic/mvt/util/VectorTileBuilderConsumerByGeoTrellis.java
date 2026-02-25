//package cn.geoair.map.dynamic.mvt.util;
//
//import cn.geoair.map.dynamic.adv.query.result.AdvOneRow;
//import cn.hutool.core.util.StrUtil;
//
//import geotrellis.vector.Extent;
//import geotrellis.vectortile.*;
//import lombok.extern.slf4j.Slf4j;
//import org.locationtech.jts.geom.*;
//import scala.Option;
//import scala.Tuple2;
//import scala.collection.JavaConverters;
//import scala.collection.Seq;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 基于Consumer模式的VectorTile PBF构建器（流式推送要素，低内存占用）
// */
//@Slf4j
//public class VectorTileBuilderConsumerByGeoTrellis extends VectorTileBuilderConsumer {
//
//    // 瓦片范围
//    private final Extent extent;
//    // 图层名称
//    private final String layerName;
//
//    private boolean hasFeature = false;
//
//    // 内部临时存储要素（逐个推送，而非一次性加载）
//    private final List<MVTFeature<Point>> tempPntList = new ArrayList<>();
//    private final List<MVTFeature<MultiPoint>> tempMultipntList = new ArrayList<>();
//    private final List<MVTFeature<LineString>> tempLineList = new ArrayList<>();
//    private final List<MVTFeature<MultiLineString>> tempMultiLineList = new ArrayList<>();
//    private final List<MVTFeature<Polygon>> tempPolyList = new ArrayList<>();
//    private final List<MVTFeature<MultiPolygon>> tempMultPolyList = new ArrayList<>();
//
//
//    private VectorTileBuilderConsumerByGeoTrellis(Envelope envelope, String layerName, int outGridSrid) {
//        this.extent = new Extent(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
//        this.layerName = layerName;
//    }
//
//    /**
//     * 创建Builder实例
//     *
//     * @param layerName 图层名称
//     * @return VectorTileBuilder
//     */
//    public static VectorTileBuilderConsumerByGeoTrellis create(Envelope envelope, String layerName, int outGridSrid) {
//        return new VectorTileBuilderConsumerByGeoTrellis(envelope, layerName, outGridSrid);
//    }
//
//
//    List<AdvOneRow> tempList = new ArrayList<>();
//
//    /**
//     * 核心方法：消费单个要素（实现Consumer接口）
//     * 逐个推送要素到Builder，无需一次性加载所有
//     */
//    @Override
//    public void accept(AdvOneRow fe) {
////        if (fe != null) {
////            tempList.add(fe);
////            if (tempList.size() > 200) {
////                doExtracted();
////            }
////        }
//        buildOne(fe);
//    }
//
////    private void doExtracted() {
////        List<AdvOneRow> collect = tempList.parallelStream().map(advOneRow -> {
////            buildOne(advOneRow);
////            return advOneRow;
////        }).collect(Collectors.toList());
////        //  验证结果（防止JVM优化）
////        if (collect.size() != tempList.size()) {
////            log.warn("并行处理数据量不一致，原大小：{}，处理后：{}", tempList.size(), collect.size());
////        }
////        tempList.clear();
////    }
//
//
//    public void buildOne(AdvOneRow fe) {
//        hasFeature = true;
//        try {
//
//            if (customTranfromConsumer != null) {
//                customTranfromConsumer.accept(fe);
//            }
//            // 1. 构建Scala属性Map（复用原逻辑）
//            scala.collection.immutable.Map<String, Value> prop = new scala.collection.immutable.HashMap<>();
//            for (String key : fe.keySet()) {
//                Object pro = fe.get(key);
//                if (pro == null) continue; // 空值跳过
//                if (pro instanceof String) {
//                    Tuple2<String, Value> kv = new Tuple2<>(key, new VString((String) pro));
//                    prop = prop.$plus(kv);
//                } else if (pro instanceof Integer) {
//                    Tuple2<String, Value> kv = new Tuple2<>(key, new VInt64(Integer.parseInt(pro.toString())));
//                    prop = prop.$plus(kv);
//                } else if (pro instanceof Double) {
//                    Tuple2<String, Value> kv = new Tuple2<>(key, new VDouble(Double.parseDouble(pro.toString())));
//                    prop = prop.$plus(kv);
//                } else if (pro instanceof Float) {
//                    Tuple2<String, Value> kv = new Tuple2<>(key, new VFloat(Float.parseFloat(pro.toString())));
//                    prop = prop.$plus(kv);
//                } else if (pro instanceof Boolean) {
//                    Tuple2<String, Value> kv = new Tuple2<>(key, new VBool(Boolean.parseBoolean(pro.toString())));
//                    prop = prop.$plus(kv);
//                } else if (pro instanceof Long) {
//                    Tuple2<String, Value> kv = new Tuple2<>(key, new VInt64(Long.parseLong(pro.toString())));
//                    prop = prop.$plus(kv);
//                }
//            }
//
//            // 2. 处理几何对象（复用原逻辑）
//            Geometry geom = (Geometry) fe.get("geom");
//            if (geom == null) {
//                log.debug("要素缺少geom字段，跳过");
//                return;
//            }
//            String type = geom.getGeometryType();
//            if (type.equalsIgnoreCase("point")) {
//                tempPntList.add(new MVTFeature<>(Option.apply(0L), (Point) geom, prop));
//            } else if (type.equalsIgnoreCase("multipoint")) {
//                tempMultipntList.add(new MVTFeature<>(Option.apply(0L), (MultiPoint) geom, prop));
//            } else if (type.equalsIgnoreCase("linestring")) {
//                tempLineList.add(new MVTFeature<>(Option.apply(0L), (LineString) geom, prop));
//            } else if (type.equalsIgnoreCase("multilinestring")) {
//                tempMultiLineList.add(new MVTFeature<>(Option.apply(0L), (MultiLineString) geom, prop));
//            } else if (type.equalsIgnoreCase("polygon")) {
//                tempPolyList.add(new MVTFeature<>(Option.apply(0L), (Polygon) geom, prop));
//            } else if (type.equalsIgnoreCase("multipolygon")) {
//                tempMultPolyList.add(new MVTFeature<>(Option.apply(0L), (MultiPolygon) geom, prop));
//            } else {
//                System.out.println("不识别的几何类型:" + type);
//            }
//
//        } finally {
//            // 关键：及时释放临时引用，帮助GC
//            fe.clear();
//            fe = null;
//        }
//    }
//
//    /**
//     * 构建最终的PBF字节数组
//     * 所有要素推送完成后调用此方法
//     */
//    public byte[] build() {
////        if (!tempList.isEmpty()) {
////            doExtracted();
////        }
//        //没有要素的时候，就直接返回，不走下面的逻辑
//        if (!hasFeature) {
//            return new byte[0];
//        }
//        try {
//            // 1. 转换List为Scala Seq（复用原逻辑）
//            Seq<MVTFeature<Point>> mvtPntSeq = JavaConverters.asScalaIteratorConverter(tempPntList.iterator()).asScala().toSeq();
//            Seq<MVTFeature<MultiPoint>> mvtMultiPntSeq = JavaConverters.asScalaIteratorConverter(tempMultipntList.iterator()).asScala().toSeq();
//            Seq<MVTFeature<LineString>> mvtLineSeq = JavaConverters.asScalaIteratorConverter(tempLineList.iterator()).asScala().toSeq();
//            Seq<MVTFeature<MultiLineString>> mvtMultilineSeq = JavaConverters.asScalaIteratorConverter(tempMultiLineList.iterator()).asScala().toSeq();
//            Seq<MVTFeature<Polygon>> mvtPolygonSeq = JavaConverters.asScalaIteratorConverter(tempPolyList.iterator()).asScala().toSeq();
//            Seq<MVTFeature<MultiPolygon>> mvtMultpolygonSeq = JavaConverters.asScalaIteratorConverter(tempMultPolyList.iterator()).asScala().toSeq();
//
//            // 2. 构建Layer和VectorTile（复用原逻辑）
//            MVTFeatures mvtFeatures = new MVTFeatures(mvtPntSeq, mvtMultiPntSeq, mvtLineSeq,
//                    mvtMultilineSeq, mvtPolygonSeq, mvtMultpolygonSeq);
//            StrictLayer layer = new StrictLayer(layerName, 4096, 1, extent, mvtFeatures);
//
//            Map<String, Layer> layers = new HashMap<>();
//            layers.put(layer.name(), layer);
//            scala.collection.mutable.Map<String, Layer> scalaLayers = JavaConverters.mapAsScalaMapConverter(layers).asScala();
//            scala.collection.immutable.HashMap<String, Layer> m = new scala.collection.immutable.HashMap<>();
//            scala.collection.Iterator<Tuple2<String, Layer>> it = scalaLayers.toList().iterator();
//            while (it.hasNext()) {
//                m = m.$plus(it.next());
//            }
//            VectorTile vt = VectorTile.apply(m, extent, false);
//
//            // 3. 生成PBF字节数组
//            byte[] pbf = new byte[0];
//            try {
//                pbf = vt.toBytes();
//            } catch (Exception e) {
//                new RuntimeException(StrUtil.format("生成失败【{}】", e.getMessage()));
//            }
//            return pbf;
//
//        } finally {
//            // 构建完成后清空内部List，释放内存
//            clear();
//        }
//    }
//
//    /**
//     * 清空内部缓存的要素（可选调用）
//     */
//    public void clear() {
//        tempPntList.clear();
//        tempMultipntList.clear();
//        tempLineList.clear();
//        tempMultiLineList.clear();
//        tempPolyList.clear();
//        tempMultPolyList.clear();
//    }
//
//
//}
