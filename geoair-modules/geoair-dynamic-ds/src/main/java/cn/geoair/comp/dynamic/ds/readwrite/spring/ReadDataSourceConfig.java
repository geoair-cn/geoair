package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 读数据源配置信息
 *
 * @author 张俊
 * @date Created in 2025/6/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadDataSourceConfig {

    /**
     * 数据源ID（唯一标识）
     */
    private String id = IdUtil.getSnowflakeNextIdStr();

    /**
     * JDBC URL
     */
    private String url;

    /**
     * 权重（用于 WEIGHT 策略，默认1）
     */
    private Integer weight = 1;

    /**
     * 是否启用（可用于动态上下线）
     */
    private Boolean enabled = true;
}
