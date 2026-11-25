package cn.geoair.comp.code.generator.multi.run;

import cn.geoair.base.Gir;
import cn.geoair.base.exception.GirException;
import cn.geoair.comp.code.generator.multi.config.GirGeneratorConfig;
import cn.geoair.comp.code.generator.multi.db.CommonRunner;
import cn.geoair.comp.code.generator.multi.db.ICommonRunner;
import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;
import cn.geoair.comp.code.generator.multi.utils.GenPathUtils;
import cn.geoair.comp.code.generator.multi.utils.GenUtils;
import cn.geoair.comp.code.generator.multi.utils.VelocityUtils;
import cn.hutool.core.map.MapUtil;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.sql.DataSource;

public class GirGenerator {

    private GirGeneratorConfig globalConfig;

    private ICommonRunner iCommonRunner;

    public GirGenerator(DataSource dataSource, GirGeneratorConfig globalConfig) {
        this.iCommonRunner = new CommonRunner(dataSource);
        this.globalConfig = globalConfig;
        VelocityUtils.initVelocity();
    }

    public GirGenerator(ICommonRunner iCommonRunner, GirGeneratorConfig globalConfig) {
        this.iCommonRunner = iCommonRunner;
        this.globalConfig = globalConfig;
        VelocityUtils.initVelocity();
    }

    public void genCode(String[] tables) {
        if (tables == null || tables.length == 0) {
            throw new GirException("生成表名列表不能为空");
        }
        // 查询表信息
        List<GenTable> tableList = iCommonRunner.selectDbTableListByNames(tables);
        for (GenTable table : tableList) {
            GenUtils.initTable(table, this.globalConfig);
            table.setPackageName(globalConfig.getSourceRootPackage());
            table.setGenPath(globalConfig.getSourceRootPath());
            // 查询列信息
            List<GenTableColumn> genTableColumns =
                    iCommonRunner.getTableColumnsByTableName(table.getTableName());
            if (genTableColumns.isEmpty()) {
                throw new GirException("表[" + table.getTableName() + "]无列信息，无法生成代码");
            }
            for (GenTableColumn column : genTableColumns) {
                GenUtils.initColumnField(column, table);
            }
            table.setColumns(genTableColumns);
            table.setProjectName(globalConfig.getProjectName());
            generatorCode(table, globalConfig);
        }
        Gir.log.info("代码生成成功");
    }

    public void genCode(String table) {
        genCode(new String[] {table});
    }

    private void generatorCode(GenTable table, GirGeneratorConfig globalConfig) {
        // 设置主键列信息
        setPkColumn(table);
        Template tpl = null;
        VelocityContext context = null;
        StringWriter sw = new StringWriter();
        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getColumns(), globalConfig);
        // 遍历模板生成代码
        for (String template : templates) {
            try {
                // 只处理枚举模板
                if (template.contains("rx-enum.java.vm")
                        || template.contains("rx-apienum.java.vm")) {
                    generateEnumCode(table, template, this.globalConfig.getMutiIs());
                } else {
                    // 渲染模板
                    context = VelocityUtils.prepareContext(table, this.globalConfig);
                    sw = new StringWriter();
                    tpl = Velocity.getTemplate(template, "UTF-8");
                    tpl.merge(context, sw);
                    String path = GenPathUtils.getGenPath(table, template, this.globalConfig);
                    FileUtils.writeStringToFile(new File(path), sw.toString(), "UTF-8");
                    Gir.log.info("生成文件：" + path);
                }
            } catch (Exception e) {
                Gir.log.error(e, "代码生成异常");
            }
        }
    }

    /** 生成枚举代码 */
    private void generateEnumCode(GenTable table, String template, Boolean mutiIs) {
        Template tpl = null;
        try {
            tpl = Velocity.getTemplate(template, "UTF-8");
        } catch (Exception e) {
            throw new GirException("加载模板失败：" + template, e);
        }

        for (GenTableColumn column : table.getColumns()) {
            if (MapUtil.isNotEmpty(column.getEnums())) {
                // 准备Velocity上下文
                VelocityContext context = VelocityUtils.prepareEnumsContext(table, column);
                // 渲染模板
                try (StringWriter sw = new StringWriter()) { // 自动关闭资源
                    tpl.merge(context, sw);
                    // 获取枚举文件生成路径
                    String enumPath = GenPathUtils.getEnumGenPath(table, column, template, mutiIs);
                    // 创建目录（如果不存在）
                    File enumFile = new File(enumPath);
                    File parentDir = enumFile.getParentFile();
                    if (!parentDir.exists()) {
                        FileUtils.forceMkdir(parentDir);
                    }
                    // 写入文件
                    FileUtils.writeStringToFile(enumFile, sw.toString(), "UTF-8");
                    Gir.log.info("生成文件：" + enumPath);
                } catch (IOException e) {
                    throw new GirException(
                            "渲染枚举模板失败，表名：" + table.getTableName() + "，列名：" + column.getColumnName(),
                            e);
                }
            }
        }
    }

    /** 设置主键列信息（修复空指针） */
    private void setPkColumn(GenTable table) {
        List<GenTableColumn> columns = table.getColumns();
        if (columns.isEmpty()) {
            throw new GirException("表[" + table.getTableName() + "]无列信息，无法设置主键");
        }
        // 查找主键列
        for (GenTableColumn column : columns) {
            if (column.isPk()) {
                table.setPkColumn(column);
                return;
            }
        }
        // 未找到主键列，取第一列
        table.setPkColumn(columns.get(0));
    }
}
