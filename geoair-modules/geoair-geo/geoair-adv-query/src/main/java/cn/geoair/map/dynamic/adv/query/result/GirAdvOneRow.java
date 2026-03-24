package cn.geoair.map.dynamic.adv.query.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.db.Entity;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/9/30 15:20 @description： 一行数据
 */
public class GirAdvOneRow extends LinkedHashMap<String, Object>
		implements OptNullGeomAndBasicTypeFromObjectGetter, Serializable {

	public static GirAdvOneRow ofByMap(Map<String, Object> row) {
		return new GirAdvOneRow(row);
	}

	public static GirAdvOneRow ofByEntity(Entity row) {
		if (row == null) {
			return new GirAdvOneRow(new LinkedHashMap<>());
		}
		return new GirAdvOneRow(row);
	}

	public <T> T toBeanObj(Class<T> clazz) {
		T bean = BeanUtil.toBean(this, clazz);
		return bean;
	}

	public static Map<String, Object> toMap(GirAdvOneRow oneRow) {
		if (oneRow == null) {
			return new LinkedHashMap<>();
		}
		return oneRow;
	}

	public static List<Map<String, Object>> toMapList(List<GirAdvOneRow> rowList) {
		if (rowList == null) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> list = new ArrayList<>();
		for (GirAdvOneRow row : rowList) {
			list.add(toMap(row));
		}
		return list;
	}

	public static List<GirAdvOneRow> ofByEntityList(List<Entity> rows) {
		if (rows == null || rows.isEmpty()) {
			return ListUtil.empty();
		}
		List<GirAdvOneRow> list = new ArrayList<>(rows.size());
		for (Entity row : rows) {
			GirAdvOneRow girAdvOneRow = ofByEntity(row);
			if (!girAdvOneRow.isEmpty()) {
				list.add(girAdvOneRow);
			}
		}
		return list;
	}

	private GirAdvOneRow(Map<String, Object> map) {
		CopyOptions copyOptions = CopyOptions.create();
		copyOptions.setIgnoreNullValue(false);
		copyOptions.setAutoTransCamelCase(false);
		BeanUtil.copyProperties(map, this, copyOptions);
	}

	private GirAdvOneRow(Entity map) {
		CopyOptions copyOptions = CopyOptions.create();
		copyOptions.setIgnoreNullValue(false);
		copyOptions.setAutoTransCamelCase(false);
		BeanUtil.copyProperties(map, this, copyOptions);
	}

	@Override
	public Object getObj(String key, Object defaultValue) {
		Object o = this.get(key);
		if (o == null) {
			return defaultValue;
		}
		return o;
	}

}
