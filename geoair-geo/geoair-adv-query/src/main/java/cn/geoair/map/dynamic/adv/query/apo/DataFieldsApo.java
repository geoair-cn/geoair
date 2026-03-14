package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yulei
 * @description
 * @create 2025年09月26日 下午2:12
 */
@Data
@NoArgsConstructor
public class DataFieldsApo implements Serializable {

	private List<FieldBySchemaApo> dataFieldList = new ArrayList<>();

	public List<FieldBySchemaApo> getDataFieldList() {
		return dataFieldList;
	}

	public void setDataFieldList(List<FieldBySchemaApo> dataFieldList) {
		if (ObjectUtil.isNotEmpty(dataFieldList)) {
			// 获取字段列表的功能，主键在前，空间字段在后，其他字段再往后
			// 获取字段列表的功能，主键在前，空间字段在后，其他字段在中间
			ListUtil.sort(dataFieldList, (f1, f2) -> {
				// 先判断是否为主键，主键优先级最高
				boolean isPk1 = f1.isPrimaryKeyIs();
				boolean isPk2 = f2.isPrimaryKeyIs();

				// 处理主键排序：主键排在非主键前面
				if (isPk1 && !isPk2) {
					return -1; // f1是主键，排在前面
				}
				else if (!isPk1 && isPk2) {
					return 1; // f2是主键，排在前面
				}

				// 都不是主键或都是主键时，处理空间字段
				boolean isGeo1 = f1.isGeometryFieldIs();
				boolean isGeo2 = f2.isGeometryFieldIs();

				// 空间字段排在最后
				if (!isGeo1 && isGeo2) {
					return -1; // f1不是空间字段，排在前面
				}
				else if (isGeo1 && !isGeo2) {
					return 1; // f2不是空间字段，排在前面
				}

				// 其他情况（都是空间字段或都不是）保持原有顺序
				return 0;
			});
			this.dataFieldList = dataFieldList;
		}
		else {
			this.dataFieldList = new ArrayList<>();
		}
	}

	public List<FieldBySchemaApo> getDataFieldList(boolean includeGeom) {
		return Collections.unmodifiableList(dataFieldList.stream().map(fieldBySchemaApo -> {
			FieldBySchemaApo copyFieldBySchemaApo = new FieldBySchemaApo();
			BeanUtil.copyProperties(fieldBySchemaApo, copyFieldBySchemaApo);
			return copyFieldBySchemaApo;
		}).filter(field -> !field.isGeometryFieldIs() || includeGeom).collect(Collectors.toList()));
	}

	public Optional<FieldBySchemaApo> getDataField(Function<FieldBySchemaApo, FieldBySchemaApo> mapper) {
		return dataFieldList.stream().map(fieldBySchemaApo -> {
			FieldBySchemaApo copyFieldBySchemaApo = new FieldBySchemaApo();
			BeanUtil.copyProperties(fieldBySchemaApo, copyFieldBySchemaApo);
			return copyFieldBySchemaApo;
		}).map(mapper).filter(Objects::nonNull).findFirst();
	}

	public <R> List<R> getFieldList(Function<FieldBySchemaApo, R> mapper, boolean includeGeom) {
		return this.dataFieldList.stream().map(fieldBySchemaApo -> {
			FieldBySchemaApo copyFieldBySchemaApo = new FieldBySchemaApo();
			BeanUtil.copyProperties(fieldBySchemaApo, copyFieldBySchemaApo);
			return copyFieldBySchemaApo;
		}).filter(field -> !field.isGeometryFieldIs() || includeGeom).map(mapper).collect(Collectors.toList());
	}

	public List<String> getFieldNameList() {
		return getFieldNameList(true);
	}

	public List<String> getFieldNameList(boolean includeGeom) {
		return getFieldList(FieldBySchemaApo::getColumnName, includeGeom);
	}

	public List<String> getFieldNameList(List<FieldBySchemaApo> dataFieldList) {
		if (ObjectUtil.isEmpty(dataFieldList)) {
			return Collections.emptyList();
		}
		return dataFieldList.stream().map(FieldBySchemaApo::getColumnName).collect(Collectors.toList());
	}

	public Optional<FieldBySchemaApo> getGeomField() {
		return this.dataFieldList.stream().map(fieldBySchemaApo -> {
			FieldBySchemaApo copyFieldBySchemaApo = new FieldBySchemaApo();
			BeanUtil.copyProperties(fieldBySchemaApo, copyFieldBySchemaApo);
			return copyFieldBySchemaApo;
		}).filter(FieldBySchemaApo::isGeometryFieldIs).findFirst();
	}

	public List<FieldBySchemaApo> getGeomFields() {
		return this.dataFieldList.stream().map(fieldBySchemaApo -> {
			FieldBySchemaApo copyFieldBySchemaApo = new FieldBySchemaApo();
			BeanUtil.copyProperties(fieldBySchemaApo, copyFieldBySchemaApo);
			return copyFieldBySchemaApo;
		}).filter(FieldBySchemaApo::isGeometryFieldIs).collect(Collectors.toList());
	}

	public List<FieldBySchemaApo> getPrimaryKeys() {
		return this.dataFieldList.stream().map(fieldBySchemaApo -> {
			FieldBySchemaApo copyFieldBySchemaApo = new FieldBySchemaApo();
			BeanUtil.copyProperties(fieldBySchemaApo, copyFieldBySchemaApo);
			return copyFieldBySchemaApo;
		}).filter(FieldBySchemaApo::isPrimaryKeyIs).collect(Collectors.toList());
	}

	public String getGeomFieldName() {
		Optional<FieldBySchemaApo> geomField = getGeomField();
		FieldBySchemaApo dataField = geomField.orElse(null);
		if (dataField != null) {
			return dataField.getColumnName();
		}
		return null;
	}

	public List<String> getGeomFieldNameList() {
		List<FieldBySchemaApo> geomFields = getGeomFields();
		return getFieldNameList(geomFields);
	}

	public List<String> getGeomUnKnownTypeFieldNameList() {
		List<FieldBySchemaApo> geomFields = getGeomFields();
		List<FieldBySchemaApo> re = new ArrayList<>();
		for (FieldBySchemaApo geomField : geomFields) {
			AdvEnumsTypeGeom geomType = geomField.getGeomType();
			if (geomType.getGeotoolsType() == null) {
				re.add(geomField);
			}
		}
		return getFieldNameList(re);
	}

	public List<String> getPrimaryKeyNameList() {
		List<FieldBySchemaApo> geomFields = getPrimaryKeys();
		return getFieldNameList(geomFields);
	}

}
