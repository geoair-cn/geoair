// package cn.geoair.orm.spi.jpa;
//
// import cn.geoair.base.Gir;
// import cn.geoair.base.log.GiLogger;
// import cn.geoair.base.log.GirLogger;
// import cn.geoair.orm.spi.GirEntityResolve;
// import cn.geoair.orm.spi.entity.GirEntityColumn;
// import cn.geoair.orm.spi.entity.GirEntityField;
// import cn.geoair.orm.spi.entity.GirEntityTable;
// import cn.geoair.orm.spi.support.GirFieldHelper;
// import cn.geoair.orm.spi.support.GirSimpleTypeUtil;
// import java.util.LinkedHashSet;
// import java.util.List;
// import jakarta.persistence.*;
//
/// **
// * jpa实体类的解析器
// *
// * @author zhangjun
// */
// public class GirJpaGirEntityResolve implements GirEntityResolve {
//
//    private final GiLogger log = GirLogger.getLoger(GirJpaGirEntityResolve.class);
//
//    @Override
//    public GirEntityTable resolveEntity(Class<?> entityClass) {
//        // 创建并缓存EntityTable
//        GirEntityTable girEntityTable = null;
//        if (entityClass.isAnnotationPresent(Table.class)) {
//            Table table = entityClass.getAnnotation(Table.class);
//            if (!"".equals(table.name())) {
//                girEntityTable = new GirEntityTable(entityClass);
//                girEntityTable.setTable(table);
//            }
//        }
//        if (girEntityTable == null) {
//            throw new RuntimeException("非po对象");
//        }
//        girEntityTable.setEntityClassColumns(new LinkedHashSet<GirEntityColumn>());
//        girEntityTable.setEntityClassPKColumns(new LinkedHashSet<GirEntityColumn>());
//        // 处理所有列
//        List<GirEntityField> fields = null;
//        fields = GirFieldHelper.getFields(entityClass);
//        for (GirEntityField field : fields) {
//            // 如果启用了简单类型，就做简单类型校验，如果不是简单类型，直接跳过
//            // 3.5.0 如果启用了枚举作为简单类型，就不会自动忽略枚举类型
//            // 4.0 如果标记了 Column 或 ColumnType 注解，也不忽略
//            if (!field.isAnnotationPresent(Column.class)
//                    && !(GirSimpleTypeUtil.isSimpleType(field.getJavaType())
//                            || (Enum.class.isAssignableFrom(field.getJavaType())))) {
//                continue;
//            }
//            processField(girEntityTable, field);
//        }
//        // 当pk.size=0的时候使用所有列作为主键
//        if (girEntityTable.getEntityClassPKColumns().size() == 0) {
//            girEntityTable.setEntityClassPKColumns(girEntityTable.getEntityClassColumns());
//        }
//        girEntityTable.initPropertyMap();
//        return girEntityTable;
//    }
//
//    @Override
//    public GirEntityTable resolveEntity(Object entityidentification) {
//        return null;
//    }
//
//    /**
//     * 处理字段
//     *
//     * @param girEntityTable
//     * @param field
//     */
//    protected void processField(GirEntityTable girEntityTable, GirEntityField field) {
//        // 排除字段
//        if (field.isAnnotationPresent(Transient.class)) {
//            return;
//        }
//        // Id
//        GirEntityColumn girEntityColumn = new GirEntityColumn(girEntityTable);
//
//        girEntityColumn.setEntityField(field);
//        if (field.isAnnotationPresent(Id.class)) {
//            girEntityColumn.setId(true);
//        }
//        // Column
//        String columnName = null;
//        if (field.isAnnotationPresent(Column.class)) {
//            Column column = field.getAnnotation(Column.class);
//            columnName = column.name();
//            girEntityColumn.setUpdatable(column.updatable());
//            girEntityColumn.setInsertable(column.insertable());
//        }
//
//        girEntityColumn.setProperty(field.getName());
//        girEntityColumn.setColumn(columnName);
//        if (columnName == null || columnName.equals("")) {
//            String entityTableField =
//                    Gir.beans
//                            .getBean(EntityManagerProvider.class)
//                            .getEntityTableField(girEntityTable.getEntityClass(),
// field.getName());
//            girEntityColumn.setColumn(entityTableField);
//        }
//        girEntityColumn.setJavaType(field.getJavaType());
//        if (field.getJavaType().isPrimitive()) {
//            log.warn(
//                    "警告信息: <["
//                            + girEntityColumn
//                            + "]> 使用了基本类型，基本类型在动态 SQL 中由于存在默认值，因此任何时候都不等于
// null，建议修改基本类型为对应的包装类型!");
//        }
//        // OrderBy
//        processOrderBy(girEntityTable, field, girEntityColumn);
//        // 处理主键策略
//        girEntityTable.getEntityClassColumns().add(girEntityColumn);
//        if (girEntityColumn.isId()) {
//            girEntityTable.getEntityClassPKColumns().add(girEntityColumn);
//        }
//    }
//
//    /**
//     * 处理排序
//     *
//     * @param girEntityTable
//     * @param field
//     * @param girEntityColumn
//     */
//    protected void processOrderBy(
//            GirEntityTable girEntityTable, GirEntityField field, GirEntityColumn girEntityColumn)
// {
//        if (field.isAnnotationPresent(OrderBy.class)) {
//            OrderBy orderBy = field.getAnnotation(OrderBy.class);
//            if ("".equals(orderBy.value())) {
//                girEntityColumn.setOrderBy("ASC");
//            } else {
//                girEntityColumn.setOrderBy(orderBy.value());
//            }
//        }
//    }
// }
