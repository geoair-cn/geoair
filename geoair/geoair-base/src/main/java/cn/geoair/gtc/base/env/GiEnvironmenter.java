package cn.geoair.gtc.base.env;

import cn.geoair.gtc.base.def.GkOperater;

public interface GiEnvironmenter extends GkOperater {


	public String[] getActiveProfiles();


	public String[] getDefaultProfiles();


	public boolean isDev();


	public boolean isDebugger();



}
