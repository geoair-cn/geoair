package cn.geoair.comp.db.service.core.common;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiPluginConfig implements Serializable {

	String apiId;

	Integer pluginType;

	String pluginName;

	String pluginParam;

}
