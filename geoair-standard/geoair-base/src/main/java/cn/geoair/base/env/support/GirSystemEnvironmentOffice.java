package cn.geoair.base.env.support;

import cn.geoair.base.def.GkOffice;
import cn.geoair.base.env.GiEnvironmenter;

public class GirSystemEnvironmentOffice implements GkOffice<GiEnvironmenter> {

    private GiEnvironmenter env = new GirSystemEnvironmentOperater();

    @Override
    public GiEnvironmenter getOperater() {
        return env;
    }
}
