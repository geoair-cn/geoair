package cn.geoair.comp.code.generator.multi.utils;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.code.generator.multi.config.GirGeneratorConfig;
import cn.geoair.comp.code.generator.multi.config.OrmType;
import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;
import cn.hutool.core.util.StrUtil;

import java.io.File;

/**
 * @author ：张逢吉
 * @date ：Created in 17:57 @description： TODO
 */
public class GenPathUtils {

    /** 项目空间路径 */
    private static final String PROJECT_PATH = "main/java";

    /** 获取枚举生成地址 */
    public static String getEnumGenPath(
            GenTable table, GenTableColumn column, String template, Boolean mutiIs) {
        String genPath = table.getGenPath();
        if (StringUtils.equals(genPath, "/")) {
            return System.getProperty("user.dir")
                    + File.separator
                    + GenPathUtils.getEnumFileName(template, table, column, mutiIs);
        }
        return genPath
                + File.separator
                + GenPathUtils.getEnumFileName(template, table, column, mutiIs);
    }

    /** 获取文件名 */
    public static String getEnumFileName(
            String template, GenTable genTable, GenTableColumn column, Boolean mutiIs) {
        // 文件名称
        String fileName = "";
        // 包路径
        String packageName = genTable.getPackageName();
        // 模块名
        String moduleName = genTable.getModuleName();
        // 枚举类名
        String enumsName = column.getEnumsName();

        fileName =
                GenPathUtils.getEnumFileNameKLF(
                        template,
                        moduleName,
                        enumsName,
                        packageName,
                        genTable.getProjectName(),
                        mutiIs);
        return fileName;
    }

    public static String getGenPath(
            GenTable table, String template, GirGeneratorConfig globalConfig) {
        String genPath = table.getGenPath();
        if (org.apache.commons.lang3.StringUtils.equals(genPath, "/")) {
            return System.getProperty("user.dir")
                    + File.separator
                    + getFileName(template, table, globalConfig);
        }
        return genPath + File.separator + getFileName(template, table, globalConfig);
    }

    /** 获取文件名 */
    private static String getFileName(
            String template, GenTable genTable, GirGeneratorConfig globalConfig) {
        // 文件名称
        String fileName = "";
        // 包路径
        String packageName = genTable.getPackageName();
        // 模块名
        String moduleName = genTable.getModuleName();
        // 大写类名
        String className = genTable.getClassName();

        String projectName = genTable.getProjectName();

        String classname =
                StringUtils.replace(StringUtils.split(genTable.getTableName(), "_", 2)[1], "_", "");

        fileName =
                GenPathUtils.getFileNameKLF(
                        template,
                        moduleName,
                        className,
                        classname,
                        packageName,
                        projectName,
                        globalConfig);

        return fileName;
    }

    /**
     * 获取普通文件生成路径（KLF规范）
     *
     * @param template 模板路径
     * @param moduleName 模块名
     * @param className 大驼峰类名
     * @param classname 小驼峰/拼接类名
     * @param packageName 包名
     * @return 完整文件路径（null表示未匹配到模板）
     */
    public static String getFileNameKLF(
            String template,
            String moduleName,
            String className,
            String classname,
            String packageName,
            String projectName,
            GirGeneratorConfig globalConfig) {
        String packagePath = StringUtils.replace(packageName, ".", "/");

        String fileName = null;
        if (globalConfig.getMutiIs()) {
            if (template.contains("rx-po.java.vm")) {
                String modelDirRoot = globalConfig.getModelDirRoot();
                if (GutilObject.isEmpty(modelDirRoot)) {
                    modelDirRoot = projectName + "-model";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/model/{}/entity/{}Po.java",
                                modelDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-po-mplus.java.vm")) {
                String modelDirRoot = globalConfig.getModelDirRoot();
                if (GutilObject.isEmpty(modelDirRoot)) {
                    modelDirRoot = projectName + "-model";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/model/{}/entity/{}Po.java",
                                modelDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-dto.java.vm")) {
                String modelDirRoot = globalConfig.getModelDirRoot();
                if (GutilObject.isEmpty(modelDirRoot)) {
                    modelDirRoot = projectName + "-model";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/model/{}/dto/{}Dto.java",
                                modelDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-seo.java.vm")) {
                String modelDirRoot = globalConfig.getModelDirRoot();
                if (GutilObject.isEmpty(modelDirRoot)) {
                    modelDirRoot = projectName + "-model";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/model/{}/seo/{}Seo.java",
                                modelDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-dao.java.vm")) {
                String daoDirRoot = globalConfig.getDaoDirRoot();
                if (GutilObject.isEmpty(daoDirRoot)) {
                    daoDirRoot = projectName + "-dao";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/dao/{}/{}Dao.java",
                                daoDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-mapper.java.vm")) {
                String mapperDirRoot = globalConfig.getMapperDirRoot();
                if (GutilObject.isEmpty(mapperDirRoot)) {
                    mapperDirRoot = projectName + "-mapper-tk";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/mapper/{}/{}Mapper.java",
                                mapperDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-mapper-plus.java.vm")) {
                String mapperDirRoot = globalConfig.getMapperDirRoot();
                if (GutilObject.isEmpty(mapperDirRoot)) {
                    mapperDirRoot = projectName + "-mybatis-plus";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/mp/{}/{}Mapper.java",
                                mapperDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-mapper.xml.vm")) {

                if (globalConfig.getOrmType().equals(OrmType.TKMAPPER)) {
                    String mapperDirRoot = globalConfig.getMapperDirRoot();
                    if (GutilObject.isEmpty(mapperDirRoot)) {
                        mapperDirRoot = projectName + "-mapper-tk";
                    }
                    fileName =
                            StringUtils.format(
                                    "{}/src/{}/{}/mapper/{}/impl/{}Mapper.xml",
                                    mapperDirRoot,
                                    PROJECT_PATH,
                                    packagePath,
                                    moduleName,
                                    className);
                } else if (globalConfig.getOrmType().equals(OrmType.MYBATISPLUS)) {
                    String mapperDirRoot = globalConfig.getMapperDirRoot();
                    if (GutilObject.isEmpty(mapperDirRoot)) {
                        mapperDirRoot = projectName + "-mybatis-plus";
                    }
                    fileName =
                            StringUtils.format(
                                    "{}/src/{}/{}/mp/{}/impl/{}Mapper.xml",
                                    mapperDirRoot,
                                    PROJECT_PATH,
                                    packagePath,
                                    moduleName,
                                    className);
                }
            } else if (template.contains("rx-servface.java.vm")) {
                String servfaceDirRoot = globalConfig.getServfaceDirRoot();
                if (GutilObject.isEmpty(servfaceDirRoot)) {
                    servfaceDirRoot = projectName + "-servface";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/servface/{}/{}Service.java",
                                servfaceDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-event.java.vm")) {
                String servfaceDirRoot = globalConfig.getServfaceDirRoot();
                if (GutilObject.isEmpty(servfaceDirRoot)) {
                    servfaceDirRoot = projectName + "-servface";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/servface/{}/event/{}Event.java",
                                servfaceDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-serviceImpl.java.vm")) {
                String serviceDirRoot = globalConfig.getServiceDirRoot();
                if (GutilObject.isEmpty(serviceDirRoot)) {
                    serviceDirRoot = projectName + "-service";
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/service/{}/{}ServiceImpl.java",
                                serviceDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-controller.java.vm")) {
                String controllerDirRoot = globalConfig.getControllerDirRoot();
                if (GutilObject.isEmpty(controllerDirRoot)) {
                    controllerDirRoot = StrUtil.format("{}-boot/{}-wcs", projectName, projectName);
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/wcs/controller/{}/{}Controller.java",
                                controllerDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-addvo.java.vm")) {
                String controllerDirRoot = globalConfig.getControllerDirRoot();
                if (GutilObject.isEmpty(controllerDirRoot)) {
                    controllerDirRoot = StrUtil.format("{}-boot/{}-wcs", projectName, projectName);
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/wcs/controller/{}/{}AddVo.java",
                                controllerDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                classname + "/" + className);
            } else if (template.contains("rx-detailvo.java.vm")) {
                String controllerDirRoot = globalConfig.getControllerDirRoot();
                if (GutilObject.isEmpty(controllerDirRoot)) {
                    controllerDirRoot = StrUtil.format("{}-boot/{}-wcs", projectName, projectName);
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/wcs/controller/{}/{}DetailVo.java",
                                controllerDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                classname + "/" + className);
            } else if (template.contains("rx-searchvo.java.vm")) {
                String controllerDirRoot = globalConfig.getControllerDirRoot();
                if (GutilObject.isEmpty(controllerDirRoot)) {
                    controllerDirRoot = StrUtil.format("{}-boot/{}-wcs", projectName, projectName);
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/wcs/controller/{}/{}SearchVo.java",
                                controllerDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                classname + "/" + className);
            } else if (template.contains("rx-updatevo.java.vm")) {
                String controllerDirRoot = globalConfig.getControllerDirRoot();
                if (GutilObject.isEmpty(controllerDirRoot)) {
                    controllerDirRoot = StrUtil.format("{}-boot/{}-wcs", projectName, projectName);
                }
                fileName =
                        StringUtils.format(
                                "{}/src/{}/{}/wcs/controller/{}/{}UpdateVo.java",
                                controllerDirRoot,
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                classname + "/" + className);
            }
        } else {
            if (template.contains("rx-po.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/model/{}/entity/{}Po.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-po-mplus.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/model/{}/entity/{}Po.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-dto.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/model/{}/dto/{}Dto.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-seo.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/model/{}/seo/{}Seo.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-dao.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/dao/{}/{}Dao.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-mapper.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/mapper/{}/{}Mapper.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-mapper-plus.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/mp/{}/{}Mapper.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-mapper.xml.vm")) {
                if (globalConfig.getOrmType().equals(OrmType.TKMAPPER)) {
                    fileName =
                            StringUtils.format(
                                    "src/{}/{}/mapper/{}/impl/{}Mapper.xml",
                                    PROJECT_PATH,
                                    packagePath,
                                    moduleName,
                                    className);
                } else if (globalConfig.getOrmType().equals(OrmType.MYBATISPLUS)) {
                    fileName =
                            StringUtils.format(
                                    "src/{}/{}/mp/{}/impl/{}Mapper.xml",
                                    PROJECT_PATH,
                                    packagePath,
                                    moduleName,
                                    className);
                }
            } else if (template.contains("rx-servface.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/servface/{}/{}Service.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-event.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/servface/{}/event/{}Event.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-serviceImpl.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/service/{}/{}ServiceImpl.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-controller.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/controller/{}/{}Controller.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                className);
            } else if (template.contains("rx-addvo.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/controller/{}/{}AddVo.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                classname + "/" + className);
            } else if (template.contains("rx-detailvo.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/controller/{}/{}DetailVo.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                classname + "/" + className);
            } else if (template.contains("rx-searchvo.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/controller/{}/{}SearchVo.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                classname + "/" + className);
            } else if (template.contains("rx-updatevo.java.vm")) {
                fileName =
                        StringUtils.format(
                                "src/{}/{}/controller/{}/{}UpdateVo.java",
                                PROJECT_PATH,
                                packagePath,
                                moduleName,
                                classname + "/" + className);
            }
        }

        return fileName;
    }

    /**
     * 获取枚举文件生成路径（KLF规范）
     *
     * @param template 模板路径
     * @param moduleName 模块名
     * @param enumsName 枚举类名
     * @param packageName 包名
     * @return 完整文件路径
     */
    public static String getEnumFileNameKLF(
            String template,
            String moduleName,
            String enumsName,
            String packageName,
            String projectName,
            Boolean mutiIs) {
        // 空值校验
        if (StringUtils.isEmpty(template)
                || StringUtils.isEmpty(enumsName)
                || StringUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("模板路径、枚举类名和包名不能为空");
        }

        // 包路径转换
        String packagePath = StringUtils.replace(packageName, ".", File.separator);

        if (template.contains("rx-enum.java.vm")) {
            if (mutiIs) {
                return StringUtils.format(
                        "{}-model/src/{}/{}/model/{}/enm/{}.java",
                        projectName,
                        PROJECT_PATH,
                        packagePath,
                        moduleName,
                        enumsName);
            } else {
                return StringUtils.format(
                        "src/{}/{}/model/{}/enm/{}.java",
                        PROJECT_PATH,
                        packagePath,
                        moduleName,
                        enumsName);
            }

        } else {
            return "";
        }
    }
}
