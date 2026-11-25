package cn.geoair.map.dynamic.mvt.dto;

import cn.hutool.core.bean.BeanUtil;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/19 14:19 @description： 瓦片执行器配置参数（新增低级别查询的两种优化策略：LIMIT 限制数量、PAGING 分页全查）
 */
@Data
@Accessors(chain = true)
public class TileExecutorConfig {

    /** 低级别数据查询优化策略 */
    private LowLevelOptStrategy lowLevelOptStrategy = LowLevelOptStrategy.NONE;

    /** 高密度的优化策略 */
    private DensityOptStrategy densityOptStrategy = DensityOptStrategy.NONE;

    /** 是否无视最小缩放级别限制 */
    private boolean ignoreMinZoom = false;

    // ---------------------- 策略1：LIMIT 直接限制查询数量 专属参数 ----------------------
    /** 小于该级别的时候启用 LIMIT 限制查询（LIMIT 策略生效） */
    private int limitStartLevel = 10;

    /** 单次查询最大返回数量（LIMIT 策略生效，直接限制数据库查询结果条数） */
    private Long maxLimitCount = 2000L;

    // ---------------------- 策略2：PAGING 分页全量查询 专属参数 ----------------------
    /** 小于该级别的时候开始分页查询（PAGING 策略生效） */
    private int pagingStartLevel = 10;

    /** 开启分页的阈值，当数据量超过该值时才启用分页（PAGING 策略生效） */
    private int pagingThreshold = 1000;

    /** 是否限制最大页号（PAGING 策略生效，防止分页过多导致性能问题） */
    private boolean limitMaxPageNumber = true;

    /** 最大页号（PAGING 策略生效，超过该页号则停止查询） */
    private Long maxPageNumber = 8L;

    /** 最大单页数据量（PAGING 策略生效，动态计算页大小时的上限，防止单页数据过多内存溢出） */
    private Long maxPageSize = 2000L;

    /**
     * 复制当前配置对象（深拷贝，避免修改副本影响原对象）
     *
     * @return 新的 TileExecutorConfig 配置对象
     */
    public TileExecutorConfig copy() {
        TileExecutorConfig newConfig = new TileExecutorConfig();
        BeanUtil.copyProperties(this, newConfig);
        return newConfig;
    }

    /**
     * 低级别数据查询优化策略枚举 NONE：不启用任何优化策略 LIMIT：直接限制单次查询返回数量（快速返回，丢弃超出数据）
     * PAGING：分页全量查询（完整获取所有数据，适合需要全量渲染的场景）
     */
    public enum LowLevelOptStrategy {
        NONE, // 无优化
        LIMIT, // 策略1：限制查询数量
        PAGING // 策略2：分页全查
    }

    /** 高密度的要素的优化策略 */
    public enum DensityOptStrategy {
        NONE, // 无优化
        DENSITY_MERGING // 高密度就合并
    }
}
