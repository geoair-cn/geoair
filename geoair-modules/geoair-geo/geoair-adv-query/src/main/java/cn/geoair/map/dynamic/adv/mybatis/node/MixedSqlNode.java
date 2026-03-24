package cn.geoair.map.dynamic.adv.mybatis.node;

import java.util.List;
import java.util.Set;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;

public class MixedSqlNode implements SqlNode {

	List<SqlNode> contents;

	public MixedSqlNode(List<SqlNode> contents) {
		this.contents = contents;
	}

	@Override
	public void apply(Context context) {
		for (SqlNode node : contents) {
			node.apply(context);
		}
	}

	@Override
	public void applyParameter(Set<String> set) {
		for (SqlNode node : contents) {
			node.applyParameter(set);
		}
	}

}
