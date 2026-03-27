package cn.geoair.orm.spi.entity;

import cn.geoair.base.util.GutilStr;
import java.util.*;
import javax.persistence.Table;

/**
 * @author ：张俊
 * @date ：Created in 2022/6/30 15:04 @description： 实体表对象
 */
public class GirEntityTable {

    // 属性和列对应
    protected Map<String, GirEntityColumn> propertyMap;

    private String name;

    private String catalog;

    private String schema;

    private String orderByClause;

    private String baseSelect;

    // 实体类 => 全部列属性
    private Set<GirEntityColumn> entityClassColumns;

    // 实体类 => 主键信息
    private Set<GirEntityColumn> entityClassPKColumns;

    // useGenerator包含多列的时候需要用到
    private List<String> keyProperties;

    private List<String> keyColumns;

    // 类
    private Class<?> entityClass;

    public GirEntityTable(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public GirEntityTable() {}

    /** 初始化 - Example 会使用 */
    public void initPropertyMap() {
        propertyMap = new HashMap<String, GirEntityColumn>(getEntityClassColumns().size());
        for (GirEntityColumn column : getEntityClassColumns()) {
            propertyMap.put(column.getProperty(), column);
        }
    }

    public String getBaseSelect() {
        return baseSelect;
    }

    public void setBaseSelect(String baseSelect) {
        this.baseSelect = baseSelect;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public Set<GirEntityColumn> getEntityClassColumns() {
        return entityClassColumns;
    }

    public void setEntityClassColumns(Set<GirEntityColumn> entityClassColumns) {
        this.entityClassColumns = entityClassColumns;
    }

    public Set<GirEntityColumn> getEntityClassPKColumns() {
        return entityClassPKColumns;
    }

    public void setEntityClassPKColumns(Set<GirEntityColumn> entityClassPKColumns) {
        this.entityClassPKColumns = entityClassPKColumns;
    }

    public String[] getKeyColumns() {
        if (keyColumns != null && keyColumns.size() > 0) {
            return keyColumns.toArray(new String[] {});
        }
        return new String[] {};
    }

    public void setKeyColumns(String keyColumn) {
        if (this.keyColumns == null) {
            this.keyColumns = new ArrayList<String>();
            this.keyColumns.add(keyColumn);
        } else {
            this.keyColumns.add(keyColumn);
        }
    }

    public String[] getKeyProperties() {
        if (keyProperties != null && keyProperties.size() > 0) {
            return keyProperties.toArray(new String[] {});
        }
        return new String[] {};
    }

    public void setKeyProperties(String keyProperty) {
        if (this.keyProperties == null) {
            this.keyProperties = new ArrayList<String>();
            this.keyProperties.add(keyProperty);
        } else {
            this.keyProperties.add(keyProperty);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getPrefix() {
        if (GutilStr.isNotEmpty(catalog)) {
            return catalog;
        }
        if (GutilStr.isNotEmpty(schema)) {
            return schema;
        }
        return "";
    }

    public Map<String, GirEntityColumn> getPropertyMap() {
        return propertyMap;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setKeyColumns(List<String> keyColumns) {
        this.keyColumns = keyColumns;
    }

    public void setKeyProperties(List<String> keyProperties) {
        this.keyProperties = keyProperties;
    }

    public void setTable(Table table) {
        this.name = table.name();
        this.catalog = table.catalog();
        this.schema = table.schema();
    }
}
