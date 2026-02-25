package cn.geoair.map.dynamic.tools.grid.converter;

import org.locationtech.jts.geom.Geometry;

/**
 * WGS84（4326）瓦片转换抽象父类
 * 提取等轴/非等轴瓦片转换的公共逻辑，子类仅实现差异化的核心计算
 */
public abstract class AbstractWgs84TileConverter extends TileConverterCommon {

    // 公共常量（4326坐标系基础参数）
    protected static final double MIN_LON = -180.0;
    protected static final double MAX_LON = 180.0;
    protected static final double MIN_LAT = -90.0;
    protected static final double MAX_LAT = 90.0;
    protected static final double MAX_VALID_LAT = 85.0511287798; // 3857有效纬度上限
    protected static final double MIN_VALID_LAT = -85.0511287798; // 3857有效纬度下限

    protected static final double PRECISION = 1e-9;               // 浮点精度补偿


    /**
     * 数值范围限制（工具方法）
     */
    protected double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }


    /**
     * 将几何图形从源坐标系转换为WGS84(4326)
     */
    protected Geometry transform(Geometry geometry, int srcSrid) {
        if (geometry == null || geometry.isEmpty()) return null;
        return sridConvertOpt.convert(geometry, srcSrid, 4326);
    }



    /**
     * 非等轴Y索引转换为等轴Y索引（4326坐标系）
     * <p>
     * 核心逻辑：
     * 1. 非等轴Y索引 → 对应纬度坐标（基于非等轴跨度：180/2^z）
     * 2. 纬度坐标 → 等轴Y索引（基于等轴跨度：360/2^z）
     * <p>
     * 注意：转换后的等轴Y索引可能是浮点数，需根据业务需求取整（默认向下取整）
     *
     * @param separateAxisY 非等轴Y索引（XYZ规范，原点左上角）
     * @param zoom          缩放级别（0-30）
     * @param roundingType  取整方式：FLOOR(向下取整)/CEIL(向上取整)/ROUND(四舍五入)
     * @return 等轴Y索引（XYZ规范，原点左上角）
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    public int convertSeparateAxisYToEqualAxisY(int separateAxisY, int zoom, RoundingType roundingType) {
        // 1. 基础参数校验
        if (zoom < 0 || zoom > 30) {
            throw new IllegalArgumentException("缩放级别不合法：zoom=" + zoom + "（合法范围0-30）");
        }
        // 非等轴Y索引的合法范围：0 ~ 2^z -1
        int separateMaxY = (1 << zoom) - 1;
        if (separateAxisY < 0 || separateAxisY > separateMaxY) {
            throw new IllegalArgumentException(
                    String.format("非等轴Y索引不合法：Y=%d, zoom=%d（合法范围0~%d）",
                            separateAxisY, zoom, separateMaxY)
            );
        }
        if (roundingType == null) {
            roundingType = RoundingType.FLOOR; // 默认向下取整
        }

        // 2. 步骤1：非等轴Y索引 → 对应的纬度坐标（顶部纬度）
        double separateLatSpan = 180.0 / (1 << zoom); // 非等轴纬度跨度：180/2^z
        double lat = MAX_LAT - separateAxisY * separateLatSpan; // 非等轴Y索引对应的顶部纬度

        // 3. 步骤2：纬度坐标 → 等轴Y索引（浮点数）
        double equalLatSpan = 360.0 / (1 << zoom); // 等轴纬度跨度：360/2^z
        double equalAxisY = (MAX_LAT - lat) / equalLatSpan; // 反向计算等轴Y索引

        // 4. 根据业务需求取整（核心：不同取整方式适配不同场景）
        int finalEqualY;
        switch (roundingType) {
            case FLOOR:
                finalEqualY = (int) Math.floor(equalAxisY);
                break;
            case CEIL:
                finalEqualY = (int) Math.ceil(equalAxisY);
                break;
            case ROUND:
                finalEqualY = (int) Math.round(equalAxisY);
                break;
            default:
                finalEqualY = (int) Math.floor(equalAxisY);
        }

        // 5. 修正等轴Y索引的合法范围（0 ~ 2^z -1）
        int equalMaxY = (1 << zoom) - 1;
        finalEqualY = Math.max(0, Math.min(finalEqualY, equalMaxY));

        return finalEqualY;
    }

    /**
     * 取整方式枚举（便于明确业务规则）
     */
    public enum RoundingType {
        FLOOR,   // 向下取整
        CEIL,    // 向上取整
        ROUND    // 四舍五入
    }

    /**
     * 反向转换：等轴Y索引 → 非等轴Y索引
     * <p>
     * 与convertSeparateAxisYToEqualAxisY互为逆运算
     *
     * @param equalAxisY   等轴Y索引（XYZ规范）
     * @param zoom         缩放级别（0-30）
     * @param roundingType 取整方式
     * @return 非等轴Y索引（XYZ规范）
     */
    public int convertEqualAxisYToSeparateAxisY(int equalAxisY, int zoom, RoundingType roundingType) {
        // 1. 参数校验
        if (zoom < 0 || zoom > 30) {
            throw new IllegalArgumentException("缩放级别不合法：zoom=" + zoom + "（合法范围0-30）");
        }
        int equalMaxY = (1 << zoom) - 1;
        if (equalAxisY < 0 || equalAxisY > equalMaxY) {
            throw new IllegalArgumentException(
                    String.format("等轴Y索引不合法：Y=%d, zoom=%d（合法范围0~%d）",
                            equalAxisY, zoom, equalMaxY)
            );
        }
        if (roundingType == null) {
            roundingType = RoundingType.FLOOR;
        }

        // 2. 等轴Y索引 → 纬度坐标
        double equalLatSpan = 360.0 / (1 << zoom);
        double lat = MAX_LAT - equalAxisY * equalLatSpan;

        // 3. 纬度坐标 → 非等轴Y索引
        double separateLatSpan = 180.0 / (1 << zoom);
        double separateAxisY = (MAX_LAT - lat) / separateLatSpan;

        // 4. 取整并修正范围
        int finalSeparateY;
        switch (roundingType) {
            case FLOOR:
                finalSeparateY = (int) Math.floor(separateAxisY);
                break;
            case CEIL:
                finalSeparateY = (int) Math.ceil(separateAxisY);
                break;
            case ROUND:
                finalSeparateY = (int) Math.round(separateAxisY);
                break;
            default:
                finalSeparateY = (int) Math.floor(separateAxisY);
        }

        int separateMaxY = (1 << zoom) - 1;
        finalSeparateY = Math.max(0, Math.min(finalSeparateY, separateMaxY));

        return finalSeparateY;
    }


    // ========== 子类需实现的差异化核心方法 ==========

    /**
     * 计算经度瓦片跨度（子类实现：等轴返回360/2^z，非等轴返回360/2^z）
     */
    protected abstract double calculateTileLonSpan(int z);

    /**
     * 计算纬度瓦片跨度（子类实现：等轴返回360/2^z，非等轴返回180/2^z）
     */
    protected abstract double calculateTileLatSpan(int z);




}
