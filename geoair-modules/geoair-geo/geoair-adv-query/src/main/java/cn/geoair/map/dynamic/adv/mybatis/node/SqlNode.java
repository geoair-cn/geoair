package cn.geoair.map.dynamic.adv.mybatis.node;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;

import java.util.Set;

public interface SqlNode {

    void apply(Context context);

    void applyParameter(Set<String> set);
}
