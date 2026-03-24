package cn.geoair.map.dynamic.geoserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import cn.geoair.map.dynamic.geoserver.beans.GsWorkspace;

import lombok.Data;

/** GeoServer 配置属性类 */
@ConfigurationProperties(prefix = "geoair.gs")
@Data
public class GirGeoServerProperties {

	/** GeoServer 数据目录路径 */
	private String dataDir;

	private GsWorkspace gsWorkspace = new GsWorkspace();

}
