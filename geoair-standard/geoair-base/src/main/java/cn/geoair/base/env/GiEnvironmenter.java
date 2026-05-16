package cn.geoair.base.env;

import cn.geoair.base.def.GkOperater;

public interface GiEnvironmenter extends GkOperater {

    String[] getActiveProfiles();

    String[] getDefaultProfiles();

    /**
     * 判断当前环境是否包含 指定profile
     *
     * @param profile 如：dev、pro、logSend
     * @return true=包含 false=不包含
     */
    boolean containsProfile(String profile);

    boolean isDev();

    boolean isDebugger();
}
