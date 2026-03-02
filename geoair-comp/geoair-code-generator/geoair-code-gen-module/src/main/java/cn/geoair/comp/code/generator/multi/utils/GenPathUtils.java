package cn.geoair.comp.code.generator.multi.utils;

import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;

import java.io.File;

/**
 * @author ：张逢吉
 * @date ：Created in   17:57
 * @description： TODO
 */
public class GenPathUtils {

    /**
     * 项目空间路径
     */
    private static final String PROJECT_PATH = "main/java" ;


    /**
     * 获取枚举生成地址
     */
    public static String getEnumGenPath(GenTable table, GenTableColumn column, String template, Boolean mutiIs) {
        String genPath = table.getGenPath();
        if (org.apache.commons.lang3.StringUtils.equals(genPath, "/" )) {
            return System.getProperty("user.dir" ) + File.separator + getFileName(template, table, mutiIs);
        }
        return genPath + File.separator + GenPathUtils.getEnumFileName(template, table, column, mutiIs);
    }

    /**
     * 获取文件名
     */
    public static String getEnumFileName(String template, GenTable genTable, GenTableColumn column, Boolean mutiIs) {
        // 文件名称
        String fileName = "" ;
        // 包路径
        String packageName = genTable.getPackageName();
        // 模块名
        String moduleName = genTable.getModuleName();
        // 枚举类名
        String enumsName = column.getEnumsName();

        fileName = GenPathUtils.getEnumFileNameKLF(template, moduleName, enumsName, packageName, genTable.getProjectName(), mutiIs);
        return fileName;
    }


    public static String getGenPath(GenTable table, String template, Boolean mutiIs) {
        String genPath = table.getGenPath();
        if (org.apache.commons.lang3.StringUtils.equals(genPath, "/" )) {
            return System.getProperty("user.dir" ) + File.separator + getFileName(template, table, mutiIs);
        }
        return genPath + File.separator + getFileName(template, table, mutiIs);
    }

    /**
     * 获取文件名
     */
    private static String getFileName(String template, GenTable genTable, Boolean mutiIs) {
        // 文件名称
        String fileName = "" ;
        // 包路径
        String packageName = genTable.getPackageName();
        // 模块名
        String moduleName = genTable.getModuleName();
        // 大写类名
        String className = genTable.getClassName();

        String projectName = genTable.getProjectName();

        String classname = StringUtils.replace(StringUtils.split(genTable.getTableName(), "_" , 2)[1], "_" , "" );

        fileName = GenPathUtils.getFileNameKLF(template, moduleName, className, classname, packageName, projectName, mutiIs);


        return fileName;
    }

    /**
     * 获取普通文件生成路径（KLF规范）
     *
     * @param template    模板路径
     * @param moduleName  模块名
     * @param className   大驼峰类名
     * @param classname   小驼峰/拼接类名
     * @param packageName 包名
     * @return 完整文件路径（null表示未匹配到模板）
     */
    public static String getFileNameKLF(String template, String moduleName, String className, String classname, String packageName, String projectName, Boolean mutiIs) {
        String packagePath = StringUtils.replace(packageName, "." , "/" );

        String fileName = null;
        if (mutiIs) {
            if (template.contains("rx-po.java.vm" )) {
                fileName = StringUtils.format("{}-model/src/{}/{}/model/{}/entity/{}Po.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-dto.java.vm" )) {
                fileName = StringUtils.format("{}-model/src/{}/{}/model/{}/dto/{}Dto.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-seo.java.vm" )) {
                fileName = StringUtils.format("{}-model/src/{}/{}/model/{}/seo/{}Seo.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-dao.java.vm" )) {
                fileName = StringUtils.format("{}-dao/src/{}/{}/dao/{}/{}Dao.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-mapper.java.vm" )) {
                fileName = StringUtils.format("{}-mapper-tk/src/{}/{}/mapper/{}/{}Mapper.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-servface.java.vm" )) {
                fileName = StringUtils.format("{}-servface/src/{}/{}/servface/{}/{}Service.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-event.java.vm" )) {
                fileName = StringUtils.format("{}-servface/src/{}/{}/servface/{}/event/{}Event.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-serviceImpl.java.vm" )) {
                fileName = StringUtils.format("{}-service/src/{}/{}/service/{}/{}ServiceImpl.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-controller.java.vm" )) {
                fileName = StringUtils.format("{}-boot/{}-wcs/src/{}/{}/controller/{}/{}Controller.java" , projectName, projectName, PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-addvo.java.vm" )) {
                fileName = StringUtils.format("{}-boot/{}-wcs/src/{}/{}/controller/{}/{}AddVo.java" , projectName, projectName, PROJECT_PATH, packagePath, moduleName, classname + "/" + className);
            } else if (template.contains("rx-detailvo.java.vm" )) {
                fileName = StringUtils.format("{}-boot/{}-wcs/src/{}/{}/controller/{}/{}DetailVo.java" , projectName, projectName, PROJECT_PATH, packagePath, moduleName, classname + "/" + className);
            } else if (template.contains("rx-searchvo.java.vm" )) {
                fileName = StringUtils.format("{}-boot/{}-wcs/src/{}/{}/controller/{}/{}SearchVo.java" , projectName, projectName, PROJECT_PATH, packagePath, moduleName, classname + "/" + className);
            } else if (template.contains("rx-updatevo.java.vm" )) {
                fileName = StringUtils.format("{}-boot/{}-wcs/src/{}/{}/controller/{}/{}UpdateVo.java" , projectName, projectName, PROJECT_PATH, packagePath, moduleName, classname + "/" + className);
            } else if (template.contains("rx-mapper.xml.vm" )) {
                fileName = StringUtils.format("{}-mapper-tk/src/{}/{}/mapper/{}/impl/{}Mapper.xml" , projectName, PROJECT_PATH, packagePath, moduleName, className);
            }
        } else {
            if (template.contains("rx-po.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/model/{}/entity/{}Po.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-dto.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/model/{}/dto/{}Dto.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-seo.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/model/{}/seo/{}Seo.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-dao.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/dao/{}/{}Dao.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-mapper.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/mapper/{}/{}Mapper.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-servface.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/servface/{}/{}Service.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-event.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/servface/{}/event/{}Event.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-serviceImpl.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/service/{}/{}ServiceImpl.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-controller.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/wcs/controller/{}/{}Controller.java" , PROJECT_PATH, packagePath, moduleName, className);
            } else if (template.contains("rx-addvo.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/wcs/controller/{}/{}AddVo.java" , PROJECT_PATH, packagePath, moduleName, classname + "/" + className);
            } else if (template.contains("rx-detailvo.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/wcs/controller/{}/{}DetailVo.java" , PROJECT_PATH, packagePath, moduleName, classname + "/" + className);
            } else if (template.contains("rx-searchvo.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/wcs/controller/{}/{}SearchVo.java" , PROJECT_PATH, packagePath, moduleName, classname + "/" + className);
            } else if (template.contains("rx-updatevo.java.vm" )) {
                fileName = StringUtils.format("src/{}/{}/wcs/controller/{}/{}UpdateVo.java" , PROJECT_PATH, packagePath, moduleName, classname + "/" + className);
            } else if (template.contains("rx-mapper.xml.vm" )) {
                fileName = StringUtils.format("src/{}/{}/mapper/{}/impl/{}Mapper.xml" , PROJECT_PATH, packagePath, moduleName, className);
            }
        }

        return fileName;
    }


    /**
     * 获取枚举文件生成路径（KLF规范）
     *
     * @param template    模板路径
     * @param moduleName  模块名
     * @param enumsName   枚举类名
     * @param packageName 包名
     * @return 完整文件路径
     */
    public static String getEnumFileNameKLF(String template, String moduleName, String enumsName, String packageName, String projectName, Boolean mutiIs) {
        // 空值校验
        if (StringUtils.isEmpty(template) || StringUtils.isEmpty(enumsName) || StringUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("模板路径、枚举类名和包名不能为空" );
        }

        // 包路径转换
        String packagePath = StringUtils.replace(packageName, "." , File.separator);

        if (template.contains("rx-enum.java.vm" )) {
            if (mutiIs) {
                return StringUtils.format("{}-model/src/{}/{}/model/{}/enm/{}.java" ,
                        projectName, PROJECT_PATH, packagePath, moduleName, enumsName);
            } else {
                return StringUtils.format("src/{}/{}/model/{}/enm/{}.java" ,
                        PROJECT_PATH, packagePath, moduleName, enumsName);
            }

        } else {
            return "" ;
        }
    }
}
