package cn.geoair.map.dynamic.file.jdbc.link;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** JDBC 空间数据读取配置。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public abstract class JdbcGeoReadLinkInfo extends JdbcGeoLinkInfo {

    /** 输入查询 SQL。 */
    protected String querySql;

    /**
     * 稳定分页排序字段或完整排序表达式。
     * <p>需要分页读取时应保证结果唯一且稳定，避免并发或重复执行时出现漏读、重读。</p>
     */
    protected String orderBy;

    @Override
    public void checkLinkInfo() {
        validateConnection();
        if (isBlank(querySql)) {
            throw new IllegalArgumentException("querySql 不能为空");
        }
        if (isBlank(orderBy)) {
            throw new IllegalArgumentException("orderBy 不能为空；JDBC 分页读取必须提供稳定排序字段");
        }
    }
}
