package cn.geoair.comp.knife4j.ext.model;

import com.fasterxml.classmate.ResolvedType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:17 @description： 类型收集模型
 */
public class SpringAddtionalModel {

	/***
	 * 第一个Type
	 */
	private ResolvedType first;

	/***
	 * 剩余
	 */
	private List<ResolvedType> remaining = new ArrayList<>();

	public ResolvedType[] getRemaining() {
		if (!remaining.isEmpty()) {
			return remaining.toArray(new ResolvedType[] {});
		}
		return new ResolvedType[] {};
	}

	public ResolvedType getFirst() {
		return first;
	}

	public void setFirst(ResolvedType first) {
		this.first = first;
	}

	public void add(ResolvedType type) {
		remaining.add(type);
	}

}
