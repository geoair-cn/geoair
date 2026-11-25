package cn.geoair.map.dynamic.tools.array;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;

import org.locationtech.jts.geom.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 几何对象与坐标数组互转接口实现类 基于JTS实现核心转换逻辑，支持指定坐标顺序，保证点顺序不变
 *
 * @author 张逢吉
 * @date 2024/12/06
 */
public class GirGeom2ArrayUtils implements GirGeom2ArrayOpt {

    // 单例实例（volatile保证可见性，防止指令重排）
    private static volatile GirGeom2ArrayUtils INSTANCE;

    /** 按 ToolsConfig 对象身份复用数组转换器，保持与其他工具入口一致。 */
    private static final Map<ToolsConfig, GirGeom2ArrayUtils> CONFIGURED_INSTANCES =
            Collections.synchronizedMap(new IdentityHashMap<ToolsConfig, GirGeom2ArrayUtils>());

    private final ToolsConfig advToolsConfig;

    public GirGeom2ArrayUtils(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig == null ? new ToolsConfig() : advToolsConfig;
    }

    public static GirGeom2ArrayUtils getInstance(ToolsConfig advToolsConfig) {
        if (advToolsConfig == null) {
            return getInstance();
        }
        synchronized (CONFIGURED_INSTANCES) {
            GirGeom2ArrayUtils arrayUtils = CONFIGURED_INSTANCES.get(advToolsConfig);
            if (arrayUtils == null) {
                arrayUtils = new GirGeom2ArrayUtils(advToolsConfig);
                CONFIGURED_INSTANCES.put(advToolsConfig, arrayUtils);
            }
            return arrayUtils;
        }
    }

    /**
     * 获取单例实例（双重校验锁）
     *
     * @return 单例对象
     */
    @Deprecated
    public static GirGeom2ArrayUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (GirGeom2ArrayUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GirGeom2ArrayUtils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public double[] pointToDoubleArray(Point point) {
        return pointToDoubleArray(point, CoordOrder.X_FIRST);
    }

    @Override
    public double[] pointToDoubleArray(Point point, CoordOrder order) {
        // 空值校验
        if (ObjectUtil.isNull(point) || point.isEmpty()) {
            return null;
        }
        // 默认值处理
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;
        Coordinate coord = point.getCoordinate();
        return CoordOrder.X_FIRST == actualOrder
                ? new double[] {coord.x, coord.y} // X在前
                : new double[] {coord.y, coord.x}; // Y在前
    }

    @Override
    public double[][] lineStringToDoubleArray(LineString lineString) {
        return lineStringToDoubleArray(lineString, CoordOrder.X_FIRST);
    }

    @Override
    public double[][] lineStringToDoubleArray(LineString lineString, CoordOrder order) {
        // 空值校验
        if (ObjectUtil.isNull(lineString) || lineString.isEmpty()) {
            return null;
        }
        // 默认值处理
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;
        Coordinate[] coords = lineString.getCoordinates();
        // 坐标数组为空返回空数组（避免NPE）
        if (ArrayUtil.isEmpty(coords)) {
            return new double[0][0];
        }
        // 转换为double二维数组，保留点顺序，按指定坐标顺序排列
        double[][] result = new double[coords.length][2];
        for (int i = 0; i < coords.length; i++) {
            if (CoordOrder.X_FIRST == actualOrder) {
                result[i][0] = coords[i].x;
                result[i][1] = coords[i].y;
            } else {
                result[i][0] = coords[i].y;
                result[i][1] = coords[i].x;
            }
        }
        return result;
    }

    @Override
    public Object geometryToDoubleArray(Geometry geometry, CoordOrder order) {
        if (ObjectUtil.isNull(geometry) || geometry.isEmpty()) {
            return null;
        }
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;
        if (geometry instanceof Point) {
            return pointToDoubleArray((Point) geometry, actualOrder);
        } else if (geometry instanceof LineString) {
            return lineStringToDoubleArray((LineString) geometry, actualOrder);
        } else {
            throw new IllegalArgumentException(
                    "仅支持Point/LineString类型，不支持：" + geometry.getGeometryType());
        }
    }

    @Override
    public Point pointByDouble(double x, double y) {
        validateFinite(x, "x");
        validateFinite(y, "y");
        return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(x, y));
    }

    @Override
    public Point pointByString(String x, String y) {
        if (x == null || y == null) {
            throw new IllegalArgumentException("x和y不能为空");
        }
        try {
            double longitude = Double.parseDouble(x.trim());
            double latitude = Double.parseDouble(y.trim());
            return pointByDouble(longitude, latitude);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("x和y必须为有效数字", e);
        }
    }

    @Override
    public Point doubleArrayToPoint(double[] coords) {
        return doubleArrayToPoint(coords, CoordOrder.X_FIRST);
    }

    @Override
    public Point doubleArrayToPoint(double[] coords, CoordOrder order) {
        return doubleArrayToPoint(coords, order, advToolsConfig.getGeometryFactory());
    }

    @Override
    public Point doubleArrayToPoint(double[] coords, CoordOrder order, GeometryFactory factory) {
        // 默认值处理
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;
        GeometryFactory actualFactory =
                ObjectUtil.isNull(factory) ? advToolsConfig.getGeometryFactory() : factory;

        // 空值/维度校验
        if (ArrayUtil.isEmpty(coords)) {
            throw new IllegalArgumentException("坐标数组不能为空，Point需要一维二维坐标 [x,y]");
        }
        if (coords.length != 2) {
            throw new IllegalArgumentException("坐标数组维度错误，Point需要一维二维坐标，当前：" + coords.length + "维");
        }

        // 数值合法性校验（排除NaN/Infinity）
        if (Double.isNaN(coords[0])
                || Double.isNaN(coords[1])
                || Double.isInfinite(coords[0])
                || Double.isInfinite(coords[1])) {
            throw new IllegalArgumentException("坐标包含非法数值：" + ArrayUtil.toString(coords));
        }

        // 按指定顺序解析为JTS的X/Y（JTS内部始终X在前）
        double x = CoordOrder.X_FIRST == actualOrder ? coords[0] : coords[1];
        double y = CoordOrder.X_FIRST == actualOrder ? coords[1] : coords[0];

        return actualFactory.createPoint(new Coordinate(x, y));
    }

    @Override
    public Point doubleArrayToPointFast(double[] coords, CoordOrder order) {
        // 默认值处理
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;

        if (ArrayUtil.isEmpty(coords)) {
            throw new IllegalArgumentException("坐标数组不能为空，Point需要一维二维坐标 [x,y]");
        }

        // 快速转换（跳过数值校验，仅维度简单判断）
        if (coords.length != 2) {
            throw new IllegalArgumentException("坐标数组维度错误，Point需要一维二维坐标");
        }

        double x = CoordOrder.X_FIRST == actualOrder ? coords[0] : coords[1];
        double y = CoordOrder.X_FIRST == actualOrder ? coords[1] : coords[0];

        return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(x, y));
    }

    @Override
    public Point doubleListToPoint(List<Double> coords) {
        return doubleArrayToPoint(toPointArray(coords));
    }

    @Override
    public Point doubleListToPoint(List<Double> coords, CoordOrder order) {
        return doubleArrayToPoint(toPointArray(coords), order);
    }

    @Override
    public Point doubleListToPoint(List<Double> coords, CoordOrder order, GeometryFactory factory) {
        return doubleArrayToPoint(toPointArray(coords), order, factory);
    }

    @Override
    public Point doubleListToPointFast(List<Double> coords, CoordOrder order) {
        return doubleArrayToPointFast(toPointArray(coords), order);
    }

    @Override
    public Geometry doubleArrayToGeometry(Object coords, CoordOrder order) {
        return doubleArrayToGeometry(coords, order, advToolsConfig.getGeometryFactory());
    }

    @Override
    public Geometry doubleArrayToGeometry(
            Object coords, CoordOrder order, GeometryFactory factory) {
        // 空值校验
        if (ObjectUtil.isNull(coords)) {
            throw new IllegalArgumentException("坐标数组不能为空");
        }

        // 识别数组类型：一维=Point，二维=LineString
        if (coords instanceof double[]) {
            return doubleArrayToPoint((double[]) coords, order, factory);
        } else if (coords instanceof double[][]) {
            return doubleArrayToLineString((double[][]) coords, order, factory);
        } else {
            throw new IllegalArgumentException(
                    "仅支持一维(double[])或二维(double[][])坐标数组，当前类型：" + coords.getClass().getName());
        }
    }

    @Override
    public LineString doubleArrayToLineString(double[][] coords) {
        return doubleArrayToLineString(coords, CoordOrder.X_FIRST);
    }

    @Override
    public LineString doubleArrayToLineString(double[][] coords, CoordOrder order) {
        return doubleArrayToLineString(coords, order, advToolsConfig.getGeometryFactory());
    }

    @Override
    public LineString doubleArrayToLineString(
            double[][] coords, CoordOrder order, GeometryFactory factory) {
        // 默认值处理
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;
        GeometryFactory actualFactory =
                ObjectUtil.isNull(factory) ? advToolsConfig.getGeometryFactory() : factory;
        // 空值处理：空数组返回空LineString
        if (ArrayUtil.isEmpty(coords)) {
            return actualFactory.createLineString();
        }
        if (coords.length < 2) {
            throw new IllegalArgumentException("LineString至少需要两个坐标点");
        }
        // 校验每个坐标点的合法性（必须是二维）
        Coordinate[] jtsCoords = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            double[] point = coords[i];
            // 坐标点维度校验
            if (ArrayUtil.isEmpty(point) || point.length != 2) {
                throw new IllegalArgumentException(
                        "第" + i + "个坐标点格式错误，必须是二维数组：" + ArrayUtil.toString(point));
            }
            // 数值合法性校验（排除NaN/Infinity）
            if (Double.isNaN(point[0])
                    || Double.isNaN(point[1])
                    || Double.isInfinite(point[0])
                    || Double.isInfinite(point[1])) {
                throw new IllegalArgumentException(
                        "第" + i + "个坐标点包含非法数值：" + ArrayUtil.toString(point));
            }
            // 按指定顺序解析为JTS的X/Y（JTS内部始终X在前）
            double x = CoordOrder.X_FIRST == actualOrder ? point[0] : point[1];
            double y = CoordOrder.X_FIRST == actualOrder ? point[1] : point[0];
            jtsCoords[i] = new Coordinate(x, y);
        }
        // 创建LineString（保留点顺序）
        return actualFactory.createLineString(jtsCoords);
    }

    @Override
    public LineString doubleArrayToLineStringFast(double[][] coords, CoordOrder order) {
        // 默认值处理
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;
        // 空值处理：空数组返回空LineString
        if (ArrayUtil.isEmpty(coords)) {
            return advToolsConfig.getGeometryFactory().createLineString();
        }
        if (coords.length < 2) {
            throw new IllegalArgumentException("LineString至少需要两个坐标点");
        }
        // 快速转换仅跳过数值校验，仍保留数组结构校验。
        Coordinate[] jtsCoords = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            double[] point = coords[i];
            if (ArrayUtil.isEmpty(point) || point.length != 2) {
                throw new IllegalArgumentException("第" + i + "个坐标点必须是二维数组");
            }
            double x = CoordOrder.X_FIRST == actualOrder ? point[0] : point[1];
            double y = CoordOrder.X_FIRST == actualOrder ? point[1] : point[0];
            jtsCoords[i] = new Coordinate(x, y);
        }
        return advToolsConfig.getGeometryFactory().createLineString(jtsCoords);
    }

    @Override
    public LineString doubleListToLineString(List<double[]> coords) {
        return doubleArrayToLineString(toLineArray(coords), CoordOrder.X_FIRST);
    }

    @Override
    public LineString doubleListToLineString(List<double[]> coords, CoordOrder order) {
        return doubleArrayToLineString(toLineArray(coords), order);
    }

    @Override
    public LineString doubleListToLineString(
            List<double[]> coords, CoordOrder order, GeometryFactory factory) {
        return doubleArrayToLineString(toLineArray(coords), order, factory);
    }

    @Override
    public LineString doubleListToLineStringFast(List<double[]> coords, CoordOrder order) {
        return doubleArrayToLineStringFast(toLineArray(coords), order);
    }

    @Override
    public Polygon doubleArrayToPolygon(double[][] shell) {
        return doubleArrayToPolygon(
                shell, null, CoordOrder.X_FIRST, advToolsConfig.getGeometryFactory());
    }

    @Override
    public Polygon doubleArrayToPolygon(
            double[][] shell, CoordOrder order, GeometryFactory factory) {
        return doubleArrayToPolygon(shell, null, order, factory);
    }

    @Override
    public Polygon doubleArrayToPolygon(
            double[][] shell, List<double[][]> holes, CoordOrder order, GeometryFactory factory) {
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;
        GeometryFactory actualFactory =
                ObjectUtil.isNull(factory) ? advToolsConfig.getGeometryFactory() : factory;
        LinearRing shellRing =
                actualFactory.createLinearRing(toRingCoordinates(shell, actualOrder, "外环"));
        LinearRing[] holeRings;
        if (holes == null || holes.isEmpty()) {
            holeRings = new LinearRing[0];
        } else {
            holeRings = new LinearRing[holes.size()];
            for (int i = 0; i < holes.size(); i++) {
                holeRings[i] =
                        actualFactory.createLinearRing(
                                toRingCoordinates(holes.get(i), actualOrder, "第" + i + "个洞"));
            }
        }
        return actualFactory.createPolygon(shellRing, holeRings);
    }

    @Override
    public double[][][] polygonToDoubleArrays(Polygon polygon, CoordOrder order) {
        if (ObjectUtil.isNull(polygon) || polygon.isEmpty()) {
            return null;
        }
        CoordOrder actualOrder = ObjectUtil.isNull(order) ? CoordOrder.X_FIRST : order;
        double[][][] rings = new double[polygon.getNumInteriorRing() + 1][][];
        rings[0] = lineStringToDoubleArray(polygon.getExteriorRing(), actualOrder);
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            rings[i + 1] = lineStringToDoubleArray(polygon.getInteriorRingN(i), actualOrder);
        }
        return rings;
    }

    /** 将坐标列表转换为 Point 所需的二维数组，并统一空值语义。 */
    private double[] toPointArray(List<Double> coords) {
        if (coords == null
                || coords.size() != 2
                || coords.get(0) == null
                || coords.get(1) == null) {
            throw new IllegalArgumentException("坐标列表必须包含两个非空数值");
        }
        return new double[] {coords.get(0), coords.get(1)};
    }

    /** 将线坐标列表转换为数组；坐标结构和数值由后续转换方法校验。 */
    private double[][] toLineArray(List<double[]> coords) {
        if (coords == null) {
            throw new IllegalArgumentException("坐标集合不能为空");
        }
        return coords.toArray(new double[0][]);
    }

    /** 将一个面环转换为闭合 Coordinate 序列。 */
    private Coordinate[] toRingCoordinates(double[][] coords, CoordOrder order, String ringName) {
        if (ArrayUtil.isEmpty(coords) || coords.length < 3) {
            throw new IllegalArgumentException(ringName + "至少需要三个坐标点");
        }
        Coordinate[] result = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            double[] point = coords[i];
            if (ArrayUtil.isEmpty(point) || point.length != 2) {
                throw new IllegalArgumentException(ringName + "第" + i + "个坐标点必须是二维数组");
            }
            validateFinite(point[0], ringName + "第" + i + "个坐标的第一个值");
            validateFinite(point[1], ringName + "第" + i + "个坐标的第二个值");
            result[i] =
                    CoordOrder.X_FIRST == order
                            ? new Coordinate(point[0], point[1])
                            : new Coordinate(point[1], point[0]);
        }
        if (!result[0].equals2D(result[result.length - 1])) {
            result = Arrays.copyOf(result, result.length + 1);
            result[result.length - 1] = new Coordinate(result[0]);
        }
        return result;
    }

    /** 校验坐标值为有限数。 */
    private void validateFinite(double value, String fieldName) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(fieldName + "必须为有限数值");
        }
    }
}
