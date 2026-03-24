package cn.geoair.map.dynamic.adv.mybatis.node;

import java.util.Set;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;

public interface SqlNode {

	void apply(Context context);

	void applyParameter(Set<String> set);

}
