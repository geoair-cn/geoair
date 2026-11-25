package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** PostGIS 链接信息类 包含数据库连接、排序字段、几何字段等核心配置 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class PostgisReadLinkInfo extends PostgisLinkInfo {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private String querySqlByOutPut; // 导出的sql

    /** 检查链接信息有效性 */
    @Override
    public void checkLinkInfo() {
        // 基础参数校验
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("PostGIS JDBC URL 不能为空");
        }
        if (username == null || password == null) {
            throw new IllegalArgumentException("数据库用户名/密码不能为空");
        }
        if (querySqlByOutPut == null || querySqlByOutPut.trim().isEmpty()) {
            throw new IllegalArgumentException("操作的SQL不能为空");
        }

        // 测试数据库连接
        try (Connection conn = getConnection()) {
            if (conn == null || conn.isClosed()) {
                throw new RuntimeException("PostGIS 数据库连接失败");
            }
            log.info("PostGIS 数据库连接测试成功，URL：{}", jdbcUrl);
        } catch (SQLException e) {
            throw new RuntimeException("PostGIS 数据库连接测试失败", e);
        }
    }
}
