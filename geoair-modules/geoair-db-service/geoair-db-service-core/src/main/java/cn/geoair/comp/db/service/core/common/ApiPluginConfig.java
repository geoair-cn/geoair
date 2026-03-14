package cn.geoair.comp.db.service.core.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ApiPluginConfig implements Serializable {

	String apiId;

	Integer pluginType;

	String pluginName;

	String pluginParam;

}
