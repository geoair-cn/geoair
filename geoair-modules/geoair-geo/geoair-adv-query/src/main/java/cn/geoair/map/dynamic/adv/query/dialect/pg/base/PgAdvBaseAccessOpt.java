package cn.geoair.map.dynamic.adv.query.dialect.pg.base;

import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PostgreSQL插入操作实现类 仅实现PG专属的差异化语法，复用父类所有通用逻辑
 */
public class PgAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public PgAdvBaseAccessOpt() {
        // 绑定MySQL专属的表名处理器
        this.dialectTableNameProcessor = PgDialectTableNameUtil.getInstance();
    }

    // PG专属常量
    private static final String PG_CONFLICT_CLAUSE = " ON CONFLICT  ";

    // PG默认主键字段
    private static final String PG_DEFAULT_PRIMARY_KEY = "id";


    @Override
    protected String buildInsertIgnoreSql(String tableName, String fields, String placeholders, List<String> conflictKeys) {
        String conflictFields = "";
        if (GutilObject.isEmpty(conflictKeys)) {
            conflictFields = String.join(",", conflictKeys);
        }
        return StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({}){}({}) DO NOTHING",
                tableName,
                fields,
                placeholders,
                PG_CONFLICT_CLAUSE,
                conflictFields);
    }


}
