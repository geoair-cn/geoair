package cn.geoair.gtc.web.module;

import java.io.Serializable;
import java.util.List;

public interface GiModule extends Serializable {

	public String moduleId();

	public String moduleName();

	// public String parentId();

	public String[] permissionIds();

	// public List<? extends gtcModule> children();

}
