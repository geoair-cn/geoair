package cn.geoair.base.env;

import cn.geoair.base.def.GkOperater;

public interface GiEnvironmenter extends GkOperater {

	public String[] getActiveProfiles();

	public String[] getDefaultProfiles();

	public boolean isDev();

	public boolean isDebugger();

}
