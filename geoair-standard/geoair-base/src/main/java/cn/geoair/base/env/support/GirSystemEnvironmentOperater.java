package cn.geoair.base.env.support;

import cn.geoair.base.env.GiEnvironmenter;

import java.lang.management.ManagementFactory;
import java.util.List;

public class GirSystemEnvironmentOperater implements GiEnvironmenter {

    @Override
    public String[] getActiveProfiles() {
        return null;
    }

    @Override
    public String[] getDefaultProfiles() {
        return null;
    }

    @Override
    public boolean containsProfile(String profile) {
        return false;
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
