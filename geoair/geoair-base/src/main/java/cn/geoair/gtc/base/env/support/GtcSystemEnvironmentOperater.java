package cn.geoair.gtc.base.env.support;

import java.lang.management.ManagementFactory;
import java.util.List;

import cn.geoair.gtc.base.env.GiEnvironmenter;

public class GtcSystemEnvironmentOperater implements GiEnvironmenter{

	@Override
	public String[] getActiveProfiles() {
		return null;
	}

	@Override
	public String[] getDefaultProfiles() {
		return null;
	}

	@Override
	public boolean isDev() {
		return false;
	}

	@Override
	public boolean isDebugger() {
		return isDebug();
	}


	public static boolean isDebug() {
		List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
		boolean isDebug = false;
		for (String arg : args) {
		  if (arg.startsWith("-Xrunjdwp") || arg.startsWith("-agentlib:jdwp")) {
			isDebug = true;
			break;
		  }
		}
		return isDebug;
	}




}
