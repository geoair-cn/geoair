package cn.geoair.map.dynamic.adv.query.apo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/11 10:41
 * @description： 数据库索引信息
 */
@Data
public class IndexApo  implements Serializable {

    /**
     * 切面名称
     */
    String schemaname;
    /**
     * 表名
     */
    String tablename;
    /**
     * 表名
     */
    String indexname;
    /**
     * 索引的定义 ：示例：CREATE INDEX v_biz_1805980491667521536_the_geom_idx ON public.v_biz_1805980491667521536 USING gist (the_geom)
     */
    String indexdef;
}
