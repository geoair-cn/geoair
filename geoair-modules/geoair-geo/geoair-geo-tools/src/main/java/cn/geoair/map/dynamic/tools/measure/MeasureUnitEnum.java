package cn.geoair.map.dynamic.tools.measure;

import lombok.Getter;

/**
 * 空间测量结果的单位。
 *
 * <p>每个单位都声明了量纲与相对基准单位的换算系数。长度以米为基准，面积以平方米为基准； 不同量纲之间不能互相转换。
 *
 * @author 张逢吉
 */
@Getter
public enum MeasureUnitEnum {

    /** 米，长度基准单位。 */
    METER(MeasureDimension.LENGTH, 1.0D),

    /** 千米。 */
    KILOMETER(MeasureDimension.LENGTH, 1000.0D),

    /** 角度近似长度，按赤道每度 111319.9 米换算，仅适合展示。 */
    DEGREE(MeasureDimension.LENGTH, 111319.9D),

    /** 国际英里。 */
    MILE(MeasureDimension.LENGTH, 1609.34D),

    /** 平方米，面积基准单位。 */
    SQUARE_METER(MeasureDimension.AREA, 1.0D),

    /** 平方千米。 */
    SQUARE_KILOMETER(MeasureDimension.AREA, 1000000.0D),

    /** 中国市亩，并非国际英亩。 */
    MU(MeasureDimension.AREA, 666.6667D),

    /** 公顷。 */
    HECTARE(MeasureDimension.AREA, 10000.0D);

    /** 单位对应的量纲。 */
    private final MeasureDimension dimension;

    /** 换算为基准单位时使用的系数。 */
    private final double toBaseFactor;

    MeasureUnitEnum(MeasureDimension dimension, double toBaseFactor) {
        this.dimension = dimension;
        this.toBaseFactor = toBaseFactor;
    }

    /** 空间测量结果的量纲。 */
    public enum MeasureDimension {
        /** 长度量纲。 */
        LENGTH,
        /** 面积量纲。 */
        AREA
    }
}
