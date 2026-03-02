package cn.geoair.comp.code.generator.multi.utils;

import cn.geoair.comp.code.generator.multi.domian.GenTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VmSourceUtils {
    /**
     * 项目空间路径
     */
    private static final String PROJECT_PATH = "main/java" ;

    /**
     * 获取模板列表（KLF规范）
     *
     * @param isEnum 是否包含枚举模板
     * @return 模板路径列表
     */
    public static List<String> getKLF(boolean isEnum) {
        List<String> templates = new ArrayList<>();
        addBaseTemplates(templates);
        if (isEnum) {
            templates.add("vm/rx/java/rx-enum.java.vm" );
        }
        return templates;
    }

    /**
     * 添加基础模板（抽离方法，提升可读性）
     */
    private static void addBaseTemplates(List<String> templates) {
        templates.add("vm/rx/java/model/rx-po.java.vm" );
        templates.add("vm/rx/java/api/rx-apo.java.vm" );
        templates.add("vm/rx/java/model/rx-dto.java.vm" );
        templates.add("vm/rx/java/model/rx-seo.java.vm" );
        templates.add("vm/rx/java/rx-controller.java.vm" );
        templates.add("vm/rx/java/rx-dao.java.vm" );
        templates.add("vm/rx/java/rx-mapper.java.vm" );
        templates.add("vm/rx/java/rx-servface.java.vm" );
        templates.add("vm/rx/java/rx-event.java.vm" );
        templates.add("vm/rx/java/rx-serviceImpl.java.vm" );
        templates.add("vm/rx/java/vo/rx-addvo.java.vm" );
        templates.add("vm/rx/java/vo/rx-detailvo.java.vm" );
        templates.add("vm/rx/java/vo/rx-searchvo.java.vm" );
        templates.add("vm/rx/java/vo/rx-updatevo.java.vm" );
        templates.add("vm/rx/xml/rx-mapper.xml.vm" );
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
    public static String getFileNameKLF(String template, String moduleName, String className, String classname, String packageName, String projectName) {
        String packagePath = StringUtils.replace(packageName, "." , "/" );

        String fileName = null;
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
        } else if (template.contains("rx-apo.java.vm" )) {
            fileName = StringUtils.format("{}-api/src/{}/{}/api/{}/apo/{}Apo.java" , projectName, PROJECT_PATH, packagePath, moduleName, className);
        } else if (template.contains("rx-mapper.xml.vm" )) {
            fileName = StringUtils.format("{}-mapper-tk/src/{}/{}/mapper/{}/impl/{}Mapper.xml" , projectName, PROJECT_PATH, packagePath, moduleName, className);
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
    public static String getEnumFileNameKLF(String template, String moduleName, String enumsName, String packageName) {
        // 空值校验
        if (StringUtils.isEmpty(template) || StringUtils.isEmpty(enumsName) || StringUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("模板路径、枚举类名和包名不能为空" );
        }
        // 处理模块名
        String safeModuleName = StringUtils.isEmpty(moduleName) ? "default" : moduleName;
        // 包路径转换
        String packagePath = StringUtils.replace(packageName, "." , File.separator);
        // 获取项目名称
        String projectName = getProjectName(packageName);
        // 拼接基础路径
        String basePath = projectName + File.separator;

        // 枚举模板匹配（互斥逻辑）
        if (template.contains("rx-enum.java.vm" )) {
            return StringUtils.format("{}model/src/{}/{}/model/{}/enm/{}.java" ,
                    basePath, PROJECT_PATH, packagePath, safeModuleName, enumsName);
        } else {
            return "" ;
        }
    }

    /**
     * 安全获取项目名称（处理包名无小数点的情况）
     *
     * @param packageName 包名
     * @return 项目名称
     */
    private static String getProjectName(String packageName) {
        if (StringUtils.isEmpty(packageName)) {
            return "default-project" ;
        }
        int lastDotIndex = packageName.lastIndexOf("." );
        if (lastDotIndex == -1 || lastDotIndex == packageName.length() - 1) {
            return packageName;
        }
        return packageName.substring(lastDotIndex + 1);
    }
}
