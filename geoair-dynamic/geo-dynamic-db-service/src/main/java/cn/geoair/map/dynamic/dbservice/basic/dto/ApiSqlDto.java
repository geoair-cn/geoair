package cn.geoair.map.dynamic.dbservice.basic.dto;

public class ApiSqlDto {

	String sqlText;

	String transformPlugin;

	String transformPluginParam;

	public String getSqlText() {
		return sqlText;
	}

	public void setSqlText(String sqlText) {
		this.sqlText = sqlText;
	}

	public String getTransformPlugin() {
		return transformPlugin;
	}

	public void setTransformPlugin(String transformPlugin) {
		this.transformPlugin = transformPlugin;
	}

	public String getTransformPluginParam() {
		return transformPluginParam;
	}

	public void setTransformPluginParam(String transformPluginParam) {
		this.transformPluginParam = transformPluginParam;
	}

}
