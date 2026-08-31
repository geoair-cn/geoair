package cn.geoair.comp.code.generator.multi.config;

import cn.hutool.core.util.StrUtil;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 13:07 @description：
 */
@Data
@Accessors(chain = true)
public class GirGeneratorConfig {

    /** 源代码生成路径 */
    String sourceRootPath = "/";

    /**
     * 获取源代码生成路径
     *
     * @return 源代码生成路径，若未设置则返回 "/"
     */
    public String getSourceRootPath() {

        if (StrUtil.isEmpty(sourceRootPath)) {
            return "/";
        }
        return sourceRootPath;
    }

    OrmType ormType = OrmType.TKMAPPER;

    /** 代码的包名的根 */
    String sourceRootPackage = "";

    /** 模块名称 */
    String moduleName = "";

    /** 项目名称 */
    String projectName = "";

    /** 控制器的包生成在哪里，默认就是 StrUtil.format("{}-boot/{}-wcs", projectName, projectName) */
    String controllerDirRoot = "";

    /** 控制器的包的模块名称 , 默认wcs */
    String controllerModuleName = "wcs";

    /** service的代码 生成在哪里，默认就是 projectName拼接service */
    String serviceDirRoot = "";

    /** servface的代码生成在哪里，默认就是 projectName拼接servface */
    String servfaceDirRoot = "";

    /** dao的代码生成在哪里，默认就是 projectName拼接dao */
    String daoDirRoot = "";

    /** 控制器的包生成在哪里，默认就是 projectName拼接mapper */
    String mapperDirRoot = "";

    /** 模型的包生成在哪里，默认就是 projectName拼接model */
    String modelDirRoot = "";

    /** 控制器接口风格，两个可选值 rest，hasType */
    ControllerStyleType controllerStyle = ControllerStyleType.hasType;

    /** 生成作者 */
    private String author = "geoair";

    /** 自动去除表前缀，默认是false */
    public boolean removePre;

    /** 表前缀(类名不会包含表前缀) */
    public String tablePrefix;

    /** 是否使用springCache生成代码 */
    public Boolean springCacheUse = true;

    /** 是否多模块 */
    private Boolean mutiIs = true;
}
