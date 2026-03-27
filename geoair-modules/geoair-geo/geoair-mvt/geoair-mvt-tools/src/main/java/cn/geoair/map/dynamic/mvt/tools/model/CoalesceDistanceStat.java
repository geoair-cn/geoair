package cn.geoair.map.dynamic.mvt.tools.model;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/4 10:05 @description： 聚类统计值 存储每个aggKey的统计信息
 */
public class CoalesceDistanceStat {

    public int point_count; // 该aggKey下聚合的点总数

    public double sqrt_point_count; // 该aggKey下point_count的平方根

    public boolean hasFirstPoint; // 标记是否已保留第一个点（用于判断clustered）

    public CoalesceDistanceStat() {
        this.point_count = 0;
        this.hasFirstPoint = false;
    }

    public void countTotal() {
        point_count++;
        sqrt_point_count = Math.sqrt(point_count);
    }

    public boolean isFirstPoint() {
        if (!hasFirstPoint) {
            hasFirstPoint = true;
            return true; // 是第一个点，保留
        }
        return false; // 非第一个点，跳过
    }

    // 更新统计并返回当前要素是否为聚类状态
    public boolean updateStat() {
        point_count++; // 每来一个点，该aggKey的总数+1
        sqrt_point_count = Math.sqrt(point_count); // 实时更新平方根
        boolean isClustered = hasFirstPoint; // 已保留第一个点 → 当前点为聚类
        if (!hasFirstPoint) {
            hasFirstPoint = true; // 第一个点标记为已保留
        }

        return isClustered;
    }
}
