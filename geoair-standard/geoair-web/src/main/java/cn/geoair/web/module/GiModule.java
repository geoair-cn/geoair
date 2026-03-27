package cn.geoair.web.module;

import java.io.Serializable;

public interface GiModule extends Serializable {

    public String moduleId();

    public String moduleName();

    // public String parentId();

    public String[] permissionIds();

    // public List<? extends gtcModule> children();

}
