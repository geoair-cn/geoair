package cn.geoair.map.dynamic.tools.array;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import org.locationtech.jts.geom.*;

import java.util.List;

/**
 * 几何对象与坐标数组互转接口实现类 基于JTS实现核心转换逻辑，支持指定坐标顺序，保证点顺序不变
 *
 * @author 张逢吉
 * @date 2024/12/06
 */
public class GirGeom2ArrayUtils implements GirGeom2ArrayOpt {


    // 单例实例（volatile保证可见性，防止指令重排）
    private static volatile GirGeom2ArrayUtils INSTANCE;

    ToolsConfig advToolsConfig;

    public GirGeom2ArrayUtils(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig;
    }

    public static GirGeom2ArrayUtils getInstance(ToolsConfig advToolsConfig) {
        return new GirGeom2ArrayUtils(advToolsConfig);
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
                ? new double[]{coord.x, coord.y} // X在前
                : new double[]{coord.y, coord.x}; // Y在前
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

        // 空值处理：空数组返回空Point
        if (ArrayUtil.isEmpty(coords)) {
            return advToolsConfig.getGeometryFactory().createPoint();
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
        double[] doubles = coords.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        return doubleArrayToPoint(doubles);
    }

    @Override
    public Point doubleListToPoint(List<Double> coords, CoordOrder order) {
        double[] doubles = coords.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        return doubleArrayToPoint(doubles, order);
    }

    @Override
    public Point doubleListToPoint(List<Double> coords, CoordOrder order, GeometryFactory factory) {
        double[] doubles = coords.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        return doubleArrayToPoint(doubles, order, factory);
    }

    @Override
    public Point doubleListToPointFast(List<Double> coords, CoordOrder order) {
        double[] doubles = coords.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        return doubleArrayToPointFast(doubles, order);
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
        // 快速转换（跳过数值校验）
        Coordinate[] jtsCoords = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            double[] point = coords[i];
            double x = CoordOrder.X_FIRST == actualOrder ? point[0] : point[1];
            double y = CoordOrder.X_FIRST == actualOrder ? point[1] : point[0];
            jtsCoords[i] = new Coordinate(x, y);
        }
        return advToolsConfig.getGeometryFactory().createLineString(jtsCoords);
    }

    @Override
    public LineString doubleListToLineString(List<double[]> coords) {
        double[][] coordsArray = coords.toArray(new double[0][]);
        return doubleArrayToLineString(coordsArray, CoordOrder.X_FIRST);
    }

    @Override
    public LineString doubleListToLineString(List<double[]> coords, CoordOrder order) {
        double[][] coordsArray = coords.toArray(new double[0][]);
        return doubleArrayToLineString(coordsArray, order);
    }

    @Override
    public LineString doubleListToLineString(List<double[]> coords, CoordOrder order, GeometryFactory factory) {
        double[][] coordsArray = coords.toArray(new double[0][]);
        return doubleArrayToLineString(coordsArray, order, factory);
    }

    @Override
    public LineString doubleListToLineStringFast(List<double[]> coords, CoordOrder order) {
        double[][] coordsArray = coords.toArray(new double[0][]);
        return doubleArrayToLineStringFast(coordsArray, order);
    }
}
