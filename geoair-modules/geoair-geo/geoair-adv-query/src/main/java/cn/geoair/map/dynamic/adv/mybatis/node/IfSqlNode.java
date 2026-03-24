package cn.geoair.map.dynamic.adv.mybatis.node;

import java.util.Set;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;

public class IfSqlNode implements SqlNode {

	String test;

	SqlNode contents;

	public IfSqlNode(String test, SqlNode contents) {
		this.test = test;
		this.contents = contents;
	}

	@Override
	public void apply(Context context) {
		Boolean value = context.getOgnlBooleanValue(test);
		if (value) {
			context.appendSql(" ");// 标签类SqlNode先拼接空格，和前面的内容隔开
			contents.apply(context);
		}

	}

	@Override
	public void applyParameter(Set<String> set) {
		contents.applyParameter(set);
	}

}
