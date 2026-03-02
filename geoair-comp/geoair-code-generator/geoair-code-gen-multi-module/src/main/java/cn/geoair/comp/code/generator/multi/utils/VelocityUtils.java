package cn.geoair.comp.code.generator.multi.utils;

import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 模板处理工具类
 *
 * @author ray
 */
public class VelocityUtils {
    /**
     * 项目空间路径
     */
    private static final String PROJECT_PATH = "main/java" ;

    /**
     * mybatis空间路径
     */
    private static final String MYBATIS_PATH = "main/resources/mapper" ;

    /**
     * 默认上级菜单，系统工具
     */
    private static final String DEFAULT_PARENT_MENU_ID = "3" ;

    /**
     * 设置枚举模板变量信息
     *
     * @return 模板列表
     */
    public static VelocityContext prepareEnumsContext(GenTable genTable, GenTableColumn genTableColumn) {
        String packageName = genTable.getPackageName();
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("packageName" , packageName);
        velocityContext.put("column" , genTableColumn);
        velocityContext.put("moduleName" , genTable.getModuleName());
        return velocityContext;
    }

    /**
     * 获取模板信息
     *
     * @return 模板列表
     */
    public static List<String> getTemplateList(List<GenTableColumn> columns) {
        boolean isEnum = false;
        for (GenTableColumn column : columns) {
            if (StringUtils.isNotEmpty(column.getEnumsName())) {
                isEnum = true;
            }
        }

        List<String> templates = new ArrayList<String>();

        templates = VmSourceUtils.getKLF(isEnum);

        return templates;
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, GenTable genTable) {
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

        fileName = VmSourceUtils.getFileNameKLF(template, moduleName, className, classname, packageName, projectName);


        return fileName;
    }


    /**
     * 获取文件名
     */
    public static String getEnumFileName(String template, GenTable genTable, GenTableColumn column) {
        // 文件名称
        String fileName = "" ;
        // 包路径
        String packageName = genTable.getPackageName();
        // 模块名
        String moduleName = genTable.getModuleName();
        // 枚举类名
        String enumsName = column.getEnumsName();


        fileName = VmSourceUtils.getEnumFileNameKLF(template, moduleName, enumsName, packageName);
        return fileName;
    }

    /**
     * 获取包前缀
     *
     * @param packageName 包名称
     * @return 包前缀名称
     */
    public static String getPackagePrefix(String packageName) {
        int lastIndex = packageName.lastIndexOf("." );
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
                importList.add("java.util.Date" );
                importList.add("com.fasterxml.jackson.annotation.JsonFormat" );
            } else if (!column.isSuperColumn() && GenConstants.TYPE_BIGDECIMAL.equals(column.getJavaType())) {
                importList.add("java.math.BigDecimal" );
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

        return StringUtils.join(dicts, ", " );
    }

    /**
     * 获取权限前缀
     *
     * @param moduleName   模块名称
     * @param businessName 业务名称
     * @return 返回权限前缀
     */
    public static String getPermissionPrefix(String moduleName, String businessName) {
        return StringUtils.format("{}:{}" , moduleName, businessName);
    }

    /**
     * 初始化vm方法
     */
    public static void initVelocity() {
        Properties p = new Properties();
        try {
            // 加载classpath目录下的vm文件
            p.setProperty("file.resource.loader.class" , "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader" );
            // 定义字符集
            p.setProperty(Velocity.INPUT_ENCODING, "UTF-8" );
            p.setProperty(Velocity.OUTPUT_ENCODING, "UTF-8" );
            // 初始化Velocity引擎，指定配置Properties
            Velocity.init(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
