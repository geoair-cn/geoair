package cn.geoair.gtc.orm.spi.jpa;

import cn.geoair.gtc.base.Gtc;
import cn.geoair.gtc.base.log.GiLoger;
import cn.geoair.gtc.base.log.GtcLoger;
import cn.geoair.gtc.orm.spi.GtcEntityResolve;
import cn.geoair.gtc.orm.spi.entity.GtcEntityColumn;
import cn.geoair.gtc.orm.spi.entity.GtcEntityField;
import cn.geoair.gtc.orm.spi.entity.GtcEntityTable;
import cn.geoair.gtc.orm.spi.support.GtcFieldHelper;
import cn.geoair.gtc.orm.spi.support.GtcSimpleTypeUtil;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * jpa实体类的解析器
 *
 * @author zhangjun
 */
public class  GtcJpaGtcEntityResolve implements GtcEntityResolve {

    private final GiLoger log =  GtcLoger.getLoger( GtcJpaGtcEntityResolve.class);

    @Override
    public GtcEntityTable resolveEntity(Class<?> entityClass) {
        //创建并缓存EntityTable
         GtcEntityTable gtcEntityTable = null;
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            if (!"".equals(table.name())) {
                 gtcEntityTable = new GtcEntityTable(entityClass);
                 gtcEntityTable.setTable(table);
            }
        }
        if ( gtcEntityTable == null) {
            throw new RuntimeException("非po对象");
        }
         gtcEntityTable.setEntityClassColumns(new LinkedHashSet<GtcEntityColumn>());
         gtcEntityTable.setEntityClassPKColumns(new LinkedHashSet<GtcEntityColumn>());
        //处理所有列
        List<GtcEntityField> fields = null;
        fields =  GtcFieldHelper.getFields(entityClass);
        for ( GtcEntityField field : fields) {
            //如果启用了简单类型，就做简单类型校验，如果不是简单类型，直接跳过
            //3.5.0 如果启用了枚举作为简单类型，就不会自动忽略枚举类型
            //4.0 如果标记了 Column 或 ColumnType 注解，也不忽略
            if (!field.isAnnotationPresent(Column.class) && !( GtcSimpleTypeUtil.isSimpleType(field.getJavaType()) || (Enum.class.isAssignableFrom(field.getJavaType())))) {
                continue;
            }
            processField( gtcEntityTable, field);
        }
        //当pk.size=0的时候使用所有列作为主键
        if ( gtcEntityTable.getEntityClassPKColumns().size() == 0) {
             gtcEntityTable.setEntityClassPKColumns( gtcEntityTable.getEntityClassColumns());
        }
         gtcEntityTable.initPropertyMap();
        return  gtcEntityTable;
    }

    @Override
    public GtcEntityTable resolveEntity(Object entityidentification) {
        return null;
    }


    /**
     * 处理字段
     *
     * @param  gtcEntityTable
     * @param field
     */
    protected void processField(GtcEntityTable gtcEntityTable, GtcEntityField field) {
        //排除字段
        if (field.isAnnotationPresent(Transient.class)) {
            return;
        }
        //Id
         GtcEntityColumn gtcEntityColumn = new GtcEntityColumn( gtcEntityTable);

         gtcEntityColumn.setEntityField(field);
        if (field.isAnnotationPresent(Id.class)) {
             gtcEntityColumn.setId(true);
        }
        //Column
        String columnName = null;
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            columnName = column.name();
             gtcEntityColumn.setUpdatable(column.updatable());
             gtcEntityColumn.setInsertable(column.insertable());
        }

         gtcEntityColumn.setProperty(field.getName());
         gtcEntityColumn.setColumn(columnName);
        if (columnName == null || columnName.equals("")) {
            String entityTableField =  Gtc.beans.getBean(EntityManagerProvider.class).getEntityTableField( gtcEntityTable.getEntityClass(), field.getName());
             gtcEntityColumn.setColumn(entityTableField);
        }
         gtcEntityColumn.setJavaType(field.getJavaType());
        if (field.getJavaType().isPrimitive()) {
            log.warn("警告信息: <[" +  gtcEntityColumn + "]> 使用了基本类型，基本类型在动态 SQL 中由于存在默认值，因此任何时候都不等于 null，建议修改基本类型为对应的包装类型!");
        }
        //OrderBy
        processOrderBy( gtcEntityTable, field,  gtcEntityColumn);
        //处理主键策略
         gtcEntityTable.getEntityClassColumns().add( gtcEntityColumn);
        if ( gtcEntityColumn.isId()) {
             gtcEntityTable.getEntityClassPKColumns().add( gtcEntityColumn);
        }
    }

    /**
     * 处理排序
     *
     * @param  gtcEntityTable
     * @param field
     * @param  gtcEntityColumn
     */
    protected void processOrderBy(GtcEntityTable gtcEntityTable, GtcEntityField field, GtcEntityColumn gtcEntityColumn) {
        if (field.isAnnotationPresent(OrderBy.class)) {
            OrderBy orderBy = field.getAnnotation(OrderBy.class);
            if ("".equals(orderBy.value())) {
                 gtcEntityColumn.setOrderBy("ASC");
            } else {
                 gtcEntityColumn.setOrderBy(orderBy.value());
            }
        }
    }
}

