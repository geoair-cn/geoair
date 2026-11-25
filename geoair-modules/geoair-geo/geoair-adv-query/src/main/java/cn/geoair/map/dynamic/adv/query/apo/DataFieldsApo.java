package cn.geoair.map.dynamic.adv.query.apo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;

import lombok.Data;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 数据库表的字段集合封装。
 *
 * <p>通过构造函数传入字段列表，自动按"主键在前 → 其他字段 → 空间字段在后"排序。 所有读方法返回的都是安全的副本，不会暴露内部可变状态。
 *
 * <pre>{@code
 * DataFieldsApo fields = new DataFieldsApo(fieldList);
 * String geomName = fields.firstGeomFieldName();
 * List<String> names = fields.fieldNames(false); // 排除几何字段
 * }</pre>
 *
 * @author yulei
 */
@Data
public class DataFieldsApo implements Serializable {

    private List<FieldBySchemaApo> dataFieldList;

    public DataFieldsApo() {
        this.dataFieldList = new ArrayList<>();
    }

    /**
     * 构造并自动按"主键 → 其他 → 空间字段"排序。
     *
     * @param fields 字段列表，可为 null（等同于空列表）
     */
    public DataFieldsApo(List<FieldBySchemaApo> fields) {
        this.dataFieldList = ObjectUtil.isNotEmpty(fields) ? fields : new ArrayList<>();
        applyDefaultSort();
    }

    // ==================== 排序规则 ====================

    /** 按"主键在前 → 其他字段 → 空间字段在后"排序。 构造函数默认执行，一般无需手动调用。 */
    public void applyDefaultSort() {
        if (ObjectUtil.isEmpty(dataFieldList)) return;
        ListUtil.sort(
                dataFieldList,
                (f1, f2) -> {
                    boolean isPk1 = f1.isPrimaryKeyIs();
                    boolean isPk2 = f2.isPrimaryKeyIs();
                    if (isPk1 && !isPk2) return -1;
                    if (!isPk1 && isPk2) return 1;

                    boolean isGeo1 = f1.isGeometryFieldIs();
                    boolean isGeo2 = f2.isGeometryFieldIs();
                    if (!isGeo1 && isGeo2) return -1;
                    if (isGeo1 && !isGeo2) return 1;

                    return 0;
                });
    }

    /** 按数据库列的自然顺序（{@code ordinalPosition}）排序。 没有 {@code ordinalPosition} 值的字段排在最后。 */
    public void applyOrdinalSort() {
        if (ObjectUtil.isEmpty(dataFieldList)) return;
        dataFieldList.sort(
                Comparator.comparing(
                        FieldBySchemaApo::getOrdinalPosition,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * 返回一个按数据库列自然顺序排序的新实例。 原实例不受影响，新实例中的字段是深拷贝。
     *
     * <pre>{@code
     * // 获取按表列序排列的字段名
     * List<String> orderedNames = fields.inOrdinalOrder().fieldNames();
     * }</pre>
     */
    public DataFieldsApo inOrdinalOrder() {
        DataFieldsApo copy = new DataFieldsApo();
        copy.dataFieldList =
                dataFieldList.stream()
                        .map(DataFieldsApo::copy)
                        .sorted(
                                Comparator.comparing(
                                        FieldBySchemaApo::getOrdinalPosition,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                        .collect(Collectors.toList());
        return copy;
    }

    // ==================== 查询方法 ====================

    /** 返回字段列表（直接引用，调用方可排序但不能增删元素）。 如需过滤请用 {@link #filterFields(boolean)}。 */
    public List<FieldBySchemaApo> getDataFieldList() {
        return dataFieldList;
    }

    /**
     * 返回过滤后的字段列表（深拷贝）。
     *
     * @param includeGeom true 保留空间字段，false 排除空间字段
     */
    public List<FieldBySchemaApo> filterFields(boolean includeGeom) {
        return dataFieldList.stream()
                .filter(f -> includeGeom || !f.isGeometryFieldIs())
                .map(DataFieldsApo::copy)
                .collect(Collectors.toList());
    }

    /**
     * 按条件查找第一个匹配的字段。
     *
     * @param predicate 匹配条件
     * @return 匹配字段的深拷贝，未找到返回 {@code Optional.empty()}
     */
    public Optional<FieldBySchemaApo> findField(Predicate<FieldBySchemaApo> predicate) {
        return dataFieldList.stream().filter(predicate).map(DataFieldsApo::copy).findFirst();
    }

    /**
     * 遍历字段并映射为自定义结果。
     *
     * @param mapper 映射函数
     * @param includeGeom true 保留空间字段，false 排除
     * @param <R> 返回值类型
     * @return 映射结果列表
     */
    public <R> List<R> mapFields(Function<FieldBySchemaApo, R> mapper, boolean includeGeom) {
        return dataFieldList.stream()
                .filter(f -> includeGeom || !f.isGeometryFieldIs())
                .map(mapper)
                .collect(Collectors.toList());
    }

    // ==================== 字段名提取 ====================

    /** 获取所有字段的列名列表 */
    public List<String> fieldNames() {
        return fieldNames(true);
    }

    /** 获取过滤后的字段列名列表 */
    public List<String> fieldNames(boolean includeGeom) {
        return mapFields(FieldBySchemaApo::getColumnName, includeGeom);
    }

    /** 从指定字段列表中提取列名（纯工具方法） */
    public static List<String> columnNamesOf(List<FieldBySchemaApo> fields) {
        if (ObjectUtil.isEmpty(fields)) return Collections.emptyList();
        return fields.stream().map(FieldBySchemaApo::getColumnName).collect(Collectors.toList());
    }

    /** 获取所有主键字段（深拷贝） */
    public List<FieldBySchemaApo> primaryKeyFields() {
        return dataFieldList.stream()
                .filter(FieldBySchemaApo::isPrimaryKeyIs)
                .map(DataFieldsApo::copy)
                .collect(Collectors.toList());
    }

    /** 获取所有主键字段名 */
    public List<String> primaryKeyFieldNames() {
        return dataFieldList.stream()
                .filter(FieldBySchemaApo::isPrimaryKeyIs)
                .map(FieldBySchemaApo::getColumnName)
                .collect(Collectors.toList());
    }

    // ==================== 空间字段相关 ====================

    /** 获取第一个空间字段（深拷贝） */
    public Optional<FieldBySchemaApo> firstGeomField() {
        return dataFieldList.stream()
                .filter(FieldBySchemaApo::isGeometryFieldIs)
                .map(DataFieldsApo::copy)
                .findFirst();
    }

    /** 获取所有空间字段（深拷贝） */
    public List<FieldBySchemaApo> geomFields() {
        return dataFieldList.stream()
                .filter(FieldBySchemaApo::isGeometryFieldIs)
                .map(DataFieldsApo::copy)
                .collect(Collectors.toList());
    }

    /** 获取第一个空间字段的列名 */
    public String firstGeomFieldName() {
        return firstGeomField().map(FieldBySchemaApo::getColumnName).orElse(null);
    }

    /** 获取所有空间字段的列名 */
    public List<String> geomFieldNames() {
        return dataFieldList.stream()
                .filter(FieldBySchemaApo::isGeometryFieldIs)
                .map(FieldBySchemaApo::getColumnName)
                .collect(Collectors.toList());
    }

    /** 获取几何类型未知的空间字段列名 */
    public List<String> unresolvedGeomTypeFieldNames() {
        return dataFieldList.stream()
                .filter(FieldBySchemaApo::isGeometryFieldIs)
                .filter(f -> f.getGeomType() != null && f.getGeomType().getGeotoolsType() == null)
                .map(FieldBySchemaApo::getColumnName)
                .collect(Collectors.toList());
    }

    // ==================== @Deprecated 旧方法（兼容） ====================

    /**
     * @deprecated 请使用 {@link #filterFields(boolean)}
     */
    @Deprecated
    public List<FieldBySchemaApo> getDataFieldList(boolean includeGeom) {
        return filterFields(includeGeom);
    }

    /**
     * @deprecated 请使用 {@link #mapFields(Function, boolean)}
     */
    @Deprecated
    public <R> List<R> getFieldList(Function<FieldBySchemaApo, R> mapper, boolean includeGeom) {
        return mapFields(mapper, includeGeom);
    }

    /**
     * @deprecated 请使用 {@link #fieldNames()}
     */
    @Deprecated
    public List<String> getFieldNameList() {
        return fieldNames(true);
    }

    /**
     * @deprecated 请使用 {@link #fieldNames(boolean)}
     */
    @Deprecated
    public List<String> getFieldNameList(boolean includeGeom) {
        return fieldNames(includeGeom);
    }

    /**
     * @deprecated 请使用 {@link #columnNamesOf(List)}
     */
    @Deprecated
    public List<String> getFieldNameList(List<FieldBySchemaApo> dataFieldList) {
        return columnNamesOf(dataFieldList);
    }

    /**
     * @deprecated 请使用 {@link #findField(Predicate)}
     */
    @Deprecated
    public Optional<FieldBySchemaApo> getDataField(
            Function<FieldBySchemaApo, FieldBySchemaApo> mapper) {
        return dataFieldList.stream()
                .filter(f -> mapper.apply(f) != null)
                .map(DataFieldsApo::copy)
                .findFirst();
    }

    /**
     * @deprecated 请使用 {@link #firstGeomField()}
     */
    @Deprecated
    public Optional<FieldBySchemaApo> getGeomField() {
        return firstGeomField();
    }

    /**
     * @deprecated 请使用 {@link #geomFields()}
     */
    @Deprecated
    public List<FieldBySchemaApo> getGeomFields() {
        return geomFields();
    }

    /**
     * @deprecated 请使用 {@link #firstGeomFieldName()}
     */
    @Deprecated
    public String getGeomFieldName() {
        return firstGeomFieldName();
    }

    /**
     * @deprecated 请使用 {@link #geomFieldNames()}
     */
    @Deprecated
    public List<String> getGeomFieldNameList() {
        return geomFieldNames();
    }

    /**
     * @deprecated 请使用 {@link #unresolvedGeomTypeFieldNames()}
     */
    @Deprecated
    public List<String> getGeomUnKnownTypeFieldNameList() {
        return unresolvedGeomTypeFieldNames();
    }

    /**
     * @deprecated 请使用 {@link #primaryKeyFields()}
     */
    @Deprecated
    public List<FieldBySchemaApo> getPrimaryKeys() {
        return primaryKeyFields();
    }

    /**
     * @deprecated 请使用 {@link #primaryKeyFieldNames()}
     */
    @Deprecated
    public List<String> getPrimaryKeyNameList() {
        return primaryKeyFieldNames();
    }

    // ==================== 内部工具 ====================

    private static FieldBySchemaApo copy(FieldBySchemaApo source) {
        FieldBySchemaApo target = new FieldBySchemaApo();
        BeanUtil.copyProperties(source, target);
        return target;
    }
}
