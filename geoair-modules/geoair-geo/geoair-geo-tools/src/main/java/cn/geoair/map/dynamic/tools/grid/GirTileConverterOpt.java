package cn.geoair.map.dynamic.tools.grid;

import cn.geoair.map.dynamic.tools.grid.converter.AbstractWgs84TileConverter;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileRange;
import cn.geoair.map.dynamic.tools.grid.dto.TileLevelMetadata;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * 瓦片转换核心接口 定义XYZ瓦片与地理范围的互转规范，支持不同坐标系扩展
 */
public interface GirTileConverterOpt {

    /**
     * XYZ瓦片转换为WKT格式的多边形范围
     *
     * @param z 缩放级别
     * @param x 瓦片X索引
     * @param y 瓦片Y索引
     * @return WKT字符串（POLYGON格式）
     */
    String xyzToWkt(int z, int x, int y, int targetSrid);

    /**
     * 将指定 Y 轴约定的瓦片转换为 WKT。
     *
     * <p>未传 {@link TileYAxis} 的旧方法固定按 Google/XYZ 顶部原点解释，
     * 以兼容已有第三方依赖。</p>
     */
    default String xyzToWkt(int z, int x, int y, TileYAxis yAxis, int targetSrid) {
        return xyzToWkt(z, x, toXyzY(z, y, yAxis), targetSrid);
    }

    /**
     * XYZ瓦片转换为瓦片范围DTO
     *
     * @param z          缩放级别
     * @param x          瓦片X索引
     * @param y          瓦片Y索引
     * @param targetSrid box网格的坐标系
     * @return 瓦片范围DTO
     */
    BoxReferencedEnvelope xyzToTileBox(int z, int x, int y, int targetSrid);

    /**
     * 将指定 Y 轴约定的瓦片转换为地理范围。
     *
     * <p>未传 {@link TileYAxis} 的旧方法固定按 Google/XYZ 顶部原点解释。</p>
     */
    default BoxReferencedEnvelope xyzToTileBox(
            int z, int x, int y, TileYAxis yAxis, int targetSrid) {
        return xyzToTileBox(z, x, toXyzY(z, y, yAxis), targetSrid);
    }

    /**
     * 地理范围转换为瓦片索引范围
     *
     * @param z       缩放级别
     * @param tileBox 地理范围DTO ，srid 默认为 opt的Srid
     * @return 瓦片索引范围DTO，四个边界均为闭区间
     */
    RangeApo tileRangeByBox(int z, Envelope tileBox);

    /**
     * 地理范围转换为瓦片索引范围
     *
     * @param z       缩放级别
     * @param tileBox 地理范围DTO ，srid 默认为 opt的Srid
     * @param srcSrid 地理范围DTO  的srid
     * @return 瓦片索引范围DTO，四个边界均为闭区间
     */
    RangeApo tileRangeByBox(int z, Envelope tileBox, int srcSrid);

    /**
     * 将几何图形转换为瓦片坐标范围
     *
     * @param z        缩放级别
     * @param geometry 几何图形对象 ，srid 默认为 opt的Srid
     * @return 瓦片坐标范围对象，最大 X/Y 为闭区间末端，遍历时应使用 {@code <= maxX/maxY}
     */
    RangeApo tileRangeByGeom(int z, Geometry geometry);

    /**
     * 将几何图形转换为瓦片坐标范围
     *
     * @param z        缩放级别
     * @param geometry 几何图形对象
     * @param srcSrid  geometry 对象的srid
     * @return 瓦片坐标范围对象，最大 X/Y 为闭区间末端，遍历时应使用 {@code <= maxX/maxY}
     */
    RangeApo tileRangeByGeom(int z, Geometry geometry, int srcSrid);

    double tileXToCoordinateX(int x, int z);

    /**
     * 获取 Google/XYZ 顶部原点 Y 行号对应的坐标值。
     */
    double tileYToCoordinateY(int y, int z);

    /**
     * 根据指定 Y 轴约定获取瓦片行的坐标值。
     */
    default double tileYToCoordinateY(int y, int z, TileYAxis yAxis) {
        if (yAxis == null) {
            throw new IllegalArgumentException("Y轴约定不能为空");
        }
        int xyzY = yAxis == TileYAxis.XYZ ? y
                : yAxis.convertBoundaryY(y, getTileRowCount(z), TileYAxis.XYZ);
        return tileYToCoordinateY(xyzY, z);
    }

    /**
     * 在 XYZ 与 TMS 之间转换 Y 行号。
     *
     * <p>总行数从当前网格层级元数据取得，因此可同时适用于 3857、等轴 4326
     * 和非等轴 4326 网格。</p>
     */
    default int convertY(int z, int y, TileYAxis sourceYAxis, TileYAxis targetYAxis) {
        if (sourceYAxis == null || targetYAxis == null) {
            throw new IllegalArgumentException("Y轴约定不能为空");
        }
        return sourceYAxis.convertY(y, getTileRowCount(z), targetYAxis);
    }

    /**
     * Google/XYZ 与 TMS 之间翻转 Y 行号。
     *
     * <p>等价于 {@code convertY(z, y, TileYAxis.XYZ, TileYAxis.TMS)}。</p>
     */
    default int reverseY(int y, int z) {
        return convertY(z, y, TileYAxis.XYZ, TileYAxis.TMS);
    }

    /**
     * 返回当前网格在指定层级的总 Y 行数。
     */
    default int getTileRowCount(int z) {
        int tileRowCount = getTileLevelMetadata(z).getNumTilesHigh();
        if (tileRowCount <= 0) {
            throw new IllegalStateException("瓦片网格总行数必须大于0");
        }
        return tileRowCount;
    }

    /**
     * 非等轴Y索引转换为等轴Y索引（4326坐标系）
     *
     * <p>核心逻辑： 1. 非等轴Y索引 → 对应纬度坐标（基于非等轴跨度：180/2^z） 2. 纬度坐标 → 等轴Y索引（基于等轴跨度：360/2^z）
     *
     * <p>注意：转换后的等轴Y索引可能是浮点数，需根据业务需求取整（默认向下取整）
     *
     * @param separateAxisY 非等轴Y索引（XYZ规范，原点左上角）
     * @param zoom          缩放级别（0-30）
     * @param roundingType  取整方式：FLOOR(向下取整)/CEIL(向上取整)/ROUND(四舍五入)
     * @return 等轴Y索引（XYZ规范，原点左上角）
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    int convertSeparateAxisYToEqualAxisY(
            int separateAxisY, int zoom, AbstractWgs84TileConverter.RoundingType roundingType);

    /**
     * 反向转换：等轴Y索引 → 非等轴Y索引
     *
     * <p>与convertSeparateAxisYToEqualAxisY互为逆运算
     *
     * @param equalAxisY   等轴Y索引（XYZ规范）
     * @param zoom         缩放级别（0-30）
     * @param roundingType 取整方式
     * @return 非等轴Y索引（XYZ规范）
     */
    int convertEqualAxisYToSeparateAxisY(
            int equalAxisY, int zoom, AbstractWgs84TileConverter.RoundingType roundingType);

    /**
     * 根据指定几何图形和缩放级别获取覆盖的瓦片列表
     *
     * @param geometry 几何图形对象
     * @param srcSrid  源坐标系EPSG代码
     * @param targetZ  目标缩放级别
     * @return 覆盖的瓦片坐标集合
     */
    Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, int targetZ);

    /**
     * 根据指定几何图形和多个缩放级别获取覆盖的瓦片列表
     *
     * @param geometry 几何图形对象
     * @param srcSrid  源坐标系EPSG代码
     * @param targetZs 目标缩放级别列表
     * @return 覆盖的瓦片坐标集合
     */
    Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, List<Integer> targetZs);

    /**
     * 根据指定几何图形和缩放级别范围获取覆盖的瓦片列表
     *
     * @param geometry 几何图形对象
     * @param srcSrid  源坐标系EPSG代码
     * @param minZ     最小缩放级别
     * @param maxZ     最大缩放级别
     * @return 覆盖的瓦片坐标集合
     */
    Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, int minZ, int maxZ);

    /**
     * 根据指定地理范围和缩放级别获取覆盖的瓦片列表
     *
     * @param envelope 地理范围对象
     * @param srcSrid  源坐标系EPSG代码
     * @param targetZ  目标缩放级别
     * @return 覆盖的瓦片坐标集合
     */
    /**
     * 默认返回 Google/XYZ 顶部原点的瓦片列表。
     */
    default Set<TileZxyApo> zxyListByBox(Envelope envelope, int srcSrid, int targetZ) {
        return zxyListByBox(envelope, srcSrid, targetZ, TileYAxis.XYZ);
    }

    Set<TileZxyApo> zxyListByBox(Envelope envelope, int srcSrid, int targetZ, TileYAxis yAxis);

    /**
     * 根据指定地理范围和多个缩放级别获取覆盖的瓦片列表
     *
     * @param envelope 地理范围对象
     * @param srcSrid  源坐标系EPSG代码
     * @param targetZs 目标缩放级别列表
     * @return 覆盖的瓦片坐标集合
     */
    /**
     * 默认返回 Google/XYZ 顶部原点的瓦片列表。
     */
    default Set<TileZxyApo> zxyListByBox(
            Envelope envelope, int srcSrid, List<Integer> targetZs) {
        return zxyListByBox(envelope, srcSrid, targetZs, TileYAxis.XYZ);
    }

    Set<TileZxyApo> zxyListByBox(
            Envelope envelope, int srcSrid, List<Integer> targetZs, TileYAxis yAxis);

    /**
     * 根据指定地理范围和缩放级别范围获取覆盖的瓦片列表
     *
     * @param envelope 地理范围对象
     * @param srcSrid  源坐标系EPSG代码
     * @param minZ     最小缩放级别
     * @param maxZ     最大缩放级别
     * @return 覆盖的瓦片坐标集合
     */
    /**
     * 默认返回 Google/XYZ 顶部原点的瓦片列表。
     */
    default Set<TileZxyApo> zxyListByBox(
            Envelope envelope, int srcSrid, int minZ, int maxZ) {
        return zxyListByBox(envelope, srcSrid, minZ, maxZ, TileYAxis.XYZ);
    }

    Set<TileZxyApo> zxyListByBox(
            Envelope envelope, int srcSrid, int minZ, int maxZ, TileYAxis yAxis);


    /**
     * 通过一组zxy获取这个zxy覆盖的大边界框
     *
     * @param zxyList
     * @return
     */
    /**
     * 默认按 Google/XYZ 顶部原点解释瓦片列表。
     */
    default BoxReferencedEnvelope boundsFromTileZxyApos(
            Set<TileZxyApo> zxyList, int targetSrid) {
        return boundsFromTileZxyApos(zxyList, TileYAxis.XYZ, targetSrid);
    }

    BoxReferencedEnvelope boundsFromTileZxyApos(
            Set<TileZxyApo> zxyList, TileYAxis yAxis, int targetSrid);

    /**
     * 根据闭区间瓦片范围计算覆盖边界。
     *
     * <p>{@link TileRange} 的最大 X/Y 为最后一个实际瓦片索引。本方法直接通过首尾
     * 瓦片边界组合范围；旧的 {@link RangeApo} 接口不经过本方法，以避免改变其既有
     * 边界语义。</p>
     *
     * @param tileRange  闭区间瓦片范围
     * @param targetSrid 目标坐标系 EPSG 代码
     * @return 覆盖范围
     */
    default BoxReferencedEnvelope boundsFromTileRange(TileRange tileRange, int targetSrid) {
        if (tileRange == null) {
            throw new IllegalArgumentException("TileRange 不能为空");
        }

        int minY = tileRange.getMinY();
        int maxY = tileRange.getMaxY();
        if (tileRange.getYAxis() == TileYAxis.TMS) {
            minY = toXyzY(tileRange.getZ(), tileRange.getMaxY(), TileYAxis.TMS);
            maxY = toXyzY(tileRange.getZ(), tileRange.getMinY(), TileYAxis.TMS);
        }
        BoxReferencedEnvelope topLeft = xyzToTileBox(
                tileRange.getZ(), tileRange.getMinX(), minY, targetSrid);
        BoxReferencedEnvelope bottomRight = xyzToTileBox(
                tileRange.getZ(), tileRange.getMaxX(), maxY, targetSrid);
        return new BoxReferencedEnvelope(new Envelope(
                topLeft.getMinX(), bottomRight.getMaxX(),
                bottomRight.getMinY(), topLeft.getMaxY()), targetSrid);
    }

    /**
     * 通过一组瓦片行列号的最大最小值获取覆盖的大边界框
     *
     * @param rangeApo
     * @return
     */
    BoxReferencedEnvelope boundsFromRangeApo(RangeApo rangeApo, int targetSrid);

    /**
     * 根据比例尺反推合适的瓦片层级
     *
     * @param targetScale   目标比例尺（例如：10000 表示 1:10000）
     * @param tilePixelSize 瓦片像素尺寸
     * @param dpi           屏幕DPI
     * @return 最合适的瓦片层级
     */
    int getZoomByScale(double targetScale, int tilePixelSize, double dpi);

    /**
     * 根据地面分辨率反推合适的瓦片层级
     *
     * @param targetResolution 目标地面分辨率（米/像素）
     * @param tilePixelSize    瓦片像素尺寸
     * @return 最合适的瓦片层级
     */
    int getZoomByResolution(double targetResolution, int tilePixelSize);

    /**
     * 批量获取多个层级的瓦片元数据
     *
     * @param minZoom       最小层级
     * @param maxZoom       最大层级
     * @param tilePixelSize 瓦片像素尺寸
     * @param dpi           屏幕DPI
     * @return 层级元数据列表
     */
    List<TileLevelMetadata> getTileLevelMetadataList(int minZoom, int maxZoom,
                                                     int tilePixelSize, double dpi);

    /**
     * 根据最大分辨率层级获取瓦片元数据（使用默认配置）
     *
     * @param maxZoom 最大分辨率层级
     * @return 瓦片层级元数据对象
     */
    TileLevelMetadata getTileLevelMetadata(int maxZoom);

    /**
     * 根据最大分辨率层级获取瓦片元数据（支持自定义瓦片尺寸和DPI）
     *
     * @param maxZoom       最大分辨率层级（最大缩放级别）
     * @param tilePixelSize 瓦片像素尺寸（例如：256、512）
     * @param dpi           屏幕DPI（例如：72、96、300）
     * @return 瓦片层级元数据对象
     */
    TileLevelMetadata getTileLevelMetadata(int maxZoom, int tilePixelSize, double dpi);

    /**
     * 将指定 Y 轴约定的行号归一为旧 API 使用的 XYZ 行号。
     */
    default int toXyzY(int z, int y, TileYAxis yAxis) {
        if (yAxis == null) {
            throw new IllegalArgumentException("Y轴约定不能为空");
        }
        // XYZ 是既有 API 的原生约定，保留原有的索引校验与边界行为。
        return yAxis == TileYAxis.XYZ ? y : convertY(z, y, yAxis, TileYAxis.XYZ);
    }

    /**
     * 将旧 API 产生的 XYZ 闭区间范围转换为目标 Y 轴约定。
     */
    default TileRange fromXyzRange(int z, TileRange xyzRange, TileYAxis yAxis) {
        if (xyzRange == null || yAxis == null) {
            throw new IllegalArgumentException("瓦片范围和Y轴约定不能为空");
        }
        return yAxis == TileYAxis.XYZ ? xyzRange : xyzRange.reverseY(getTileRowCount(z));
    }

    /**
     * 将旧 API 产生的 XYZ 瓦片列表转换为目标 Y 轴约定。
     */
    default Set<TileZxyApo> fromXyzTiles(Set<TileZxyApo> xyzTiles, TileYAxis yAxis) {
        if (xyzTiles == null || yAxis == null) {
            throw new IllegalArgumentException("瓦片列表和Y轴约定不能为空");
        }
        if (yAxis == TileYAxis.XYZ) {
            return xyzTiles;
        }
        Set<TileZxyApo> converted = new LinkedHashSet<>();
        for (TileZxyApo tile : xyzTiles) {
            converted.add(new TileZxyApo(tile.getZ(), tile.getX(),
                    convertY(tile.getZ(), tile.getY(), TileYAxis.XYZ, TileYAxis.TMS)));
        }
        return converted;
    }

    /**
     * 将指定 Y 轴约定的瓦片列表归一为旧 API 使用的 XYZ 瓦片列表。
     */
    default Set<TileZxyApo> toXyzTiles(Set<TileZxyApo> tiles, TileYAxis yAxis) {
        if (tiles == null || yAxis == null) {
            throw new IllegalArgumentException("瓦片列表和Y轴约定不能为空");
        }
        if (yAxis == TileYAxis.XYZ) {
            return tiles;
        }
        Set<TileZxyApo> converted = new LinkedHashSet<>();
        for (TileZxyApo tile : tiles) {
            converted.add(new TileZxyApo(tile.getZ(), tile.getX(), toXyzY(tile.getZ(), tile.getY(), TileYAxis.TMS)));
        }
        return converted;
    }
}
