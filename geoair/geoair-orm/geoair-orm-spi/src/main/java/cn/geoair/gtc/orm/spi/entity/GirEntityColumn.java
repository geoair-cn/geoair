
package cn.geoair.gtc.orm.spi.entity;

/**
 * 数据库表对应的列
 *
 * @author zhangjun
 */
public class GirEntityColumn {

	//
	protected GirEntityTable table;

	// 字段名称
	protected String property;

	// 字段对应的数据库表名
	protected String column;

	//
	protected Class<?> javaType;

	//

	protected boolean id = false;

	protected boolean identity = false;

	// private Class<? extends GenId> genIdClass;
	// 字段是否为 blob
	protected boolean blob;

	protected String generator;

	// 排序
	protected String orderBy;

	// 可插入
	private boolean insertable = true;

	// 可更新
	private boolean updatable = true;

	// private ORDER order = ORDER.DEFAULT;
	// 是否设置 javaType
	private boolean useJavaType;

	/**
	 * 对应的字段信息
	 *
	 * @since 3.5.0
	 */
	private GirEntityField girEntityField;

	public GirEntityColumn() {
	}

	public GirEntityColumn(GirEntityTable table) {
		this.table = table;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		GirEntityColumn that = (GirEntityColumn) o;

		if (id != that.id)
			return false;
		if (identity != that.identity)
			return false;
		if (table != null ? !table.equals(that.table) : that.table != null)
			return false;
		if (property != null ? !property.equals(that.property) : that.property != null)
			return false;
		if (column != null ? !column.equals(that.column) : that.column != null)
			return false;
		if (javaType != null ? !javaType.equals(that.javaType) : that.javaType != null)
			return false;

		if (generator != null ? !generator.equals(that.generator) : that.generator != null)
			return false;
		return !(orderBy != null ? !orderBy.equals(that.orderBy) : that.orderBy != null);

	}

	@Override
	public int hashCode() {
		int result = table != null ? table.hashCode() : 0;
		result = 31 * result + (property != null ? property.hashCode() : 0);
		result = 31 * result + (column != null ? column.hashCode() : 0);
		result = 31 * result + (javaType != null ? javaType.hashCode() : 0);

		result = 31 * result + (id ? 1 : 0);
		result = 31 * result + (identity ? 1 : 0);
		result = 31 * result + (generator != null ? generator.hashCode() : 0);
		result = 31 * result + (orderBy != null ? orderBy.hashCode() : 0);
		return result;
	}

	public String getColumn() {
		return column;
	}

	public String getSelectColumn() {
		return column;
	}

	/**
	 * 包装 插入或者 更新的 value
	 * @param value
	 * @return
	 */
	public Object wrapColumnValue(Object value) {
		return value;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public GirEntityField getEntityField() {
		return girEntityField;
	}

	public void setEntityField(GirEntityField girEntityField) {
		this.girEntityField = girEntityField;
	}

	public String getGenerator() {
		return generator;
	}

	public void setGenerator(String generator) {
		this.generator = generator;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public void setJavaType(Class<?> javaType) {
		this.javaType = javaType;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public GirEntityTable getTable() {
		return table;
	}

	public void setTable(GirEntityTable table) {
		this.table = table;
	}

	// public Class<? extends TypeHandler<?>> getTypeHandler() {
	// return typeHandler;
	// }
	//
	// public void setTypeHandler(Class<? extends TypeHandler<?>> typeHandler) {
	// this.typeHandler = typeHandler;
	// }

	public boolean isId() {
		return id;
	}

	public void setId(boolean id) {
		this.id = id;
	}

	public boolean isIdentity() {
		return identity;
	}

	public void setIdentity(boolean identity) {
		this.identity = identity;
	}

	public boolean isInsertable() {
		return insertable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public boolean isUpdatable() {
		return updatable;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public boolean isBlob() {
		return blob;
	}

	public void setBlob(boolean blob) {
		this.blob = blob;
	}

	public boolean isUseJavaType() {
		return useJavaType;
	}

	public void setUseJavaType(boolean useJavaType) {
		this.useJavaType = useJavaType;
	}

	@Override
	public String toString() {
		return "EntityColumn{" + "table=" + table.getName() + ", property='" + property + '\'' + ", column='" + column
				+ '\'' + ", javaType=" + javaType + ", id=" + id + ", identity=" + identity + ", blob=" + blob
				+ ", generator='" + generator + '\'' + ", orderBy='" + orderBy + '\'' + ", insertable=" + insertable
				+ ", updatable=" + updatable +
				// ", order=" + order +
				'}';
	}

}
