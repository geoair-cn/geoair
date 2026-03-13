package cn.geoair.comp.code.generator.multi.utils;

import cn.geoair.comp.code.generator.multi.config.GirGeneratorConfig;
import cn.geoair.comp.code.generator.multi.config.OrmType;
import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;
import cn.hutool.core.date.DateUtil;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.util.*;

/**
 * 模板处理工具类
 *
 * @author ray
 */
public class VelocityUtils {

    /**
     * 设置枚举模板变量信息
     *
     * @return 模板列表
     */
    public static VelocityContext prepareEnumsContext(
            GenTable genTable, GenTableColumn genTableColumn) {
        String packageName = genTable.getPackageName();
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("packageName", packageName);
        velocityContext.put("column", genTableColumn);
        velocityContext.put("moduleName", genTable.getModuleName());
        return velocityContext;
    }

    public static VelocityContext prepareContext(GenTable genTable, GirGeneratorConfig globalConfig) {

        String packageName = genTable.getPackageName();

        String functionName = genTable.getFunctionName();

        VelocityContext velocityContext = new VelocityContext();

        velocityContext.put("tableName", genTable.getTableName());
        velocityContext.put(
                "ormPackge", globalConfig.getOrmType().equals(OrmType.MYBATISPLUS) ? "mp" : "mapper");
        velocityContext.put(
                "functionName", StringUtils.isNotEmpty(functionName) ? functionName : "【请填写功能名称】");
        velocityContext.put("ClassName", genTable.getClassName());
        velocityContext.put("className", StringUtils.uncapitalize(genTable.getClassName()));
        velocityContext.put("enableCache", globalConfig.springCacheUse);
        // velocityContext.put("classname",
        // StringUtils.replace(genTable.getTableName(),"_",""));

        velocityContext.put(
                "classname",
                StringUtils.replace(StringUtils.split(genTable.getTableName(), "_", 2)[1], "_", ""));

        velocityContext.put("moduleName", genTable.getModuleName());
        velocityContext.put("BusinessName", StringUtils.capitalize(genTable.getBusinessName()));
        velocityContext.put("businessName", genTable.getBusinessName());
        velocityContext.put("basePackage", getPackagePrefix(packageName));
        velocityContext.put("packageName", packageName);
        velocityContext.put("author", genTable.getFunctionAuthor());
        velocityContext.put("datetime", DateUtil.now());
        velocityContext.put("pkColumn", genTable.getPkColumn());
        velocityContext.put("importList", getImportList(genTable));
        velocityContext.put("columns", genTable.getColumns());
        velocityContext.put("table", genTable);
        velocityContext.put("dicts", getDicts(genTable));
        velocityContext.put("tableComment", genTable.getTableComment());
        velocityContext.put("time", new Date().getTime());

        return velocityContext;
    }

    /**
     * 获取模板信息
     *
     * @return 模板列表
     */
    public static List<String> getTemplateList(
            List<GenTableColumn> columns, GirGeneratorConfig globalConfig) {
        boolean isEnum = false;
        for (GenTableColumn column : columns) {
            if (StringUtils.isNotEmpty(column.getEnumsName())) {
                isEnum = true;
            }
        }

        List<String> templates = new ArrayList<String>();

        templates = getKLF(isEnum, globalConfig);

        return templates;
    }

    /**
     * 获取模板列表（KLF规范）
     *
     * @param isEnum 是否包含枚举模板
     * @return 模板路径列表
     */
    public static List<String> getKLF(boolean isEnum, GirGeneratorConfig globalConfig) {
        List<String> templates = new ArrayList<>();
        addBaseTemplates(templates, globalConfig);
        if (isEnum) {
            templates.add("vm/java/rx-enum.java.vm");
        }
        return templates;
    }

    /**
     * 添加基础模板（抽离方法，提升可读性）
     */
    private static void addBaseTemplates(List<String> templates, GirGeneratorConfig globalConfig) {
        if (globalConfig.getOrmType().equals(OrmType.MYBATISPLUS)) {
            templates.add("vm/java/model/rx-po-mplus.java.vm");
            templates.add("vm/java/rx-mapper-plus.java.vm");
        } else {
            templates.add("vm/java/model/rx-po.java.vm");
            templates.add("vm/java/rx-mapper.java.vm");
        }
        templates.add("vm/java/model/rx-dto.java.vm");
        templates.add("vm/java/model/rx-seo.java.vm");
        templates.add("vm/java/rx-controller.java.vm");
        templates.add("vm/java/rx-dao.java.vm");
        templates.add("vm/java/rx-servface.java.vm");
        templates.add("vm/java/rx-event.java.vm");
        templates.add("vm/java/rx-serviceImpl.java.vm");
        templates.add("vm/java/vo/rx-addvo.java.vm");
        templates.add("vm/java/vo/rx-detailvo.java.vm");
        templates.add("vm/java/vo/rx-searchvo.java.vm");
        templates.add("vm/java/vo/rx-updatevo.java.vm");
        templates.add("vm/xml/rx-mapper.xml.vm");
    }

    /**
     * 获取包前缀
     *
     * @param packageName 包名称
     * @return 包前缀名称
     */
    public static String getPackagePrefix(String packageName) {
        int lastIndex = packageName.lastIndexOf(".");
        String basePackage = StringUtils.substring(packageName, 0, lastIndex);
        return basePackage;
    }

    /**
     * 根据列类型获取导入包
     *
     * @param genTable 业务表对象
     * @return 返回需要导入的包列表
     */
    public static HashSet<String> getImportList(GenTable genTable) {
        List<GenTableColumn> columns = genTable.getColumns();

        HashSet<String> importList = new HashSet<String>();
        for (GenTableColumn column : columns) {
            if (!column.isSuperColumn() && GenConstants.TYPE_DATE.equals(column.getJavaType())) {
                importList.add("java.util.Date");
                importList.add("com.fasterxml.jackson.annotation.JsonFormat");
            } else if (!column.isSuperColumn()
                    && GenConstants.TYPE_BIGDECIMAL.equals(column.getJavaType())) {
                importList.add("java.math.BigDecimal");
            } else if (!column.isSuperColumn()
                    && GenConstants.TYPE_Geometry.equals(column.getJavaType())) {
                importList.add("org.locationtech.jts.geom.Geometry");
            }
        }
        return importList;
    }

    /**
     * 根据列类型获取字典组
     *
     * @param genTable 业务表对象
     * @return 返回字典组
     */
    public static String getDicts(GenTable genTable) {
        List<GenTableColumn> columns = genTable.getColumns();
        List<String> dicts = new ArrayList<String>();

        return StringUtils.join(dicts, ", ");
    }

    /**
     * 初始化vm方法
     */
    public static void initVelocity() {
        Properties p = new Properties();
        try {
            // 加载classpath目录下的vm文件
            p.setProperty(
                    "file.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            // 定义字符集
            p.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
            p.setProperty(Velocity.OUTPUT_ENCODING, "UTF-8");
            // 初始化Velocity引擎，指定配置Properties
            Velocity.init(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
