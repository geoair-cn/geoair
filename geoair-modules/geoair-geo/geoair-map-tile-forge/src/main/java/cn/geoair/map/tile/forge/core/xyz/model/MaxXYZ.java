package cn.geoair.map.tile.forge.core.xyz.model;

/**
 * @author ：张俊
 * @date ： Created in 2025/11/19 15:59
 * @description ：用于存储瓦片坐标最大值的实体类
 */
public class MaxXYZ {
    /**
     * X方向上的最大瓦片编号
     */
    private Integer maxX;

    /**
     * Y方向上的最大瓦片编号
     */
    private Integer maxY;

    /**
     * Z方向（缩放级别）上的最大值
     */
    private Integer maxZ;

    /**
     * Z方向（缩放级别）上的最小值
     */
    private Integer minZ;


    public MaxXYZ() {
    }

    public MaxXYZ(Integer maxX, Integer maxY, Integer maxZ) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public Integer getMinZ() {
        return minZ;
    }

    public void setMinZ(Integer minZ) {
        this.minZ = minZ;
    }

    public Integer getMaxX() {
        return maxX;
    }

    public void setMaxX(Integer maxX) {
        this.maxX = maxX;
    }

    public Integer getMaxY() {
        return maxY;
    }

    public void setMaxY(Integer maxY) {
        this.maxY = maxY;
    }

    public Integer getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(Integer maxZ) {
        this.maxZ = maxZ;
    }

    public MaxXYZ initDefaultValue() {
        maxX = 0;
        maxY = 0;
        maxZ = 18;
        minZ = 0;
        return this;
    }

}
