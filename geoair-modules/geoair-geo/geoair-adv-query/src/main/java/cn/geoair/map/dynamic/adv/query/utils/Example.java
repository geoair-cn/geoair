package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.map.dynamic.adv.query.apo.QueryRequest;
import cn.geoair.map.dynamic.adv.query.apo.QueryFilter;

import java.util.Arrays;

public class Example {

    public static void main(String[] args) {

        // 示例1：简单条件 (name = '张三' AND age > 18)
        QueryFilter where1 = QueryFilter.of()
                .eq("name", "张三")
                .gt("age", 18);

        // 示例2：AND条件组 (status = 1 AND (age > 18 AND age < 30))
        QueryFilter where2 = QueryFilter.of()
                .eq("status", 1)
                .andGroup(builder -> builder
                        .gt("age", 18)
                        .lt("age", 30)
                );

        // 示例3：OR条件组 (status = 1 OR status = 2)
        QueryFilter where3 = QueryFilter.of()
                .orGroup(builder -> builder
                        .eq("status", 1)
                        .eq("status", 2)
                );

        // 示例4：复杂嵌套 (name LIKE '%张%' AND (age > 18 OR status = 1))
        QueryFilter where4 = QueryFilter.of()
                .like("name", "张")
                .andGroup(builder -> builder
                        .gt("age", 18)
                        .orGroup(nested -> nested
                                .eq("status", 1).gt("age", 30)
                        )
                );

        // 示例5：更复杂的嵌套 ((age > 18 AND age < 30) OR (status IN (1,2) AND name LIKE '%张%'))
        QueryFilter where5 = QueryFilter.of()
                .orGroup(builder -> builder
                        .andGroup(nested -> nested
                                .gt("age", 18)
                                .lt("age", 30)
                        )
                        .andGroup(nested -> nested
                                .in("status", Arrays.asList(1, 2))
                                .like("name", "张")
                        )
                );

        // 示例6：NOT条件组 (NOT (status = 0))
        QueryFilter where6 = QueryFilter.of()
                .notGroup(builder -> builder
                        .eq("status", 0)
                );

        // 示例7：混合使用
        QueryFilter where7 = QueryFilter.of()
                .eq("deleted", 0)
                .andGroup(builder -> builder
                        .gt("age", 18)
                        .orGroup(nested -> nested
                                .eq("vip", 1)
                                .gt("score", 100)
                        )
                )
                .like("name", "张");

        // 使用SelectQueryParam
        QueryRequest query = QueryRequest.builder()
                .table("user")
                .fields("id", "name", "age")
                .where(where5)
                .orderByDesc("create_time")
                .page(1, 20)
                .build();

        // 生成SQL


        QueryRequest build = QueryRequest.builder().table("testTanme")
                .where(QueryFilter.of().ge("外面", "")
                        .andGroup(group -> {
                            group.isNotNull("andGroup").eq("andGroup", "cc");
                        }).orGroup(or -> {
                            or.isNotNull("orGroup").eq("orGroup", "cc");
                        })
                )
                .fields("*").build();
        QueryRequest build1 = QueryRequest.builder()
                .table("testTanme").fields("*")
                .where(QueryFilter.of()
                        .ge("外面", "")
                        .andGroup(group -> {
                            group.isNotNull("andGroup").eq("andGroup", "cc");
                        })
                        .orGroup(or -> {
                            or.isNotNull("orGroup").eq("orGroup", "cc");
                        })
                )
                .build();
        QuerySqlBuilder.SqlBuildResult result = QuerySqlBuilder.buildPageSql(build1);
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
    }

//    SELECT * FROM testTanme WHERE 外面 >= ?  AND ( andGroup IS NOT NULL AND andGroup = ? ) or (orGroup IS NOT NULL AND orGroup = ?))
}
