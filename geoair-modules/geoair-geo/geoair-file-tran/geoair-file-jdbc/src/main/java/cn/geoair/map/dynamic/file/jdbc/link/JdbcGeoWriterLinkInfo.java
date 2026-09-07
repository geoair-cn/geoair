package cn.geoair.map.dynamic.file.jdbc.link;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/** JDBC 空间数据写入配置。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public abstract class JdbcGeoWriterLinkInfo extends JdbcGeoLinkInfo {

    /** 目标表名。 */
    protected String tableName;

    /** 默认批量写入大小。 */
    protected int batchSize = 1000;

    /** 冲突判断字段；为空时由数据库执行普通插入。 */
    protected List<String> conflictKeys = new ArrayList<>();

    @Override
    public void checkLinkInfo() {
        validateConnection();
        if (isBlank(tableName)) {
            throw new IllegalArgumentException("tableName 不能为空");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于 0");
        }
    }
}
