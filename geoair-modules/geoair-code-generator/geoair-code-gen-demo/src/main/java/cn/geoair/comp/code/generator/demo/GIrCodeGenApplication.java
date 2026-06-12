package cn.geoair.comp.code.generator.demo;

import cn.geoair.comp.code.generator.multi.config.GirGeneratorConfig;
import cn.geoair.comp.code.generator.multi.run.GirGenerator;
import cn.geoair.comp.dynamic.ds.simple.DriverManagerDataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;

@SpringBootApplication
// @ComponentScan(value = "com.gir")
public class GIrCodeGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(GIrCodeGenApplication.class, args);
        DataSource dataSource =
                new DriverManagerDataSource(
                        "jdbc:postgresql://192.168.0.110:5432/editor_dev", "postgres", "tcsd2019");
        GirGeneratorConfig globalConfig = new GirGeneratorConfig();
        globalConfig
                .setAuthor("geoair")
                .setModuleName("mm")
                .setProjectName("Pp")
                .setRemovePre(true)
                .setSourceRootPackage("com.gg.ccc.root")
                .setSourceRootPath("")
                .setTablePrefix("");
        GirGenerator generator = new GirGenerator(dataSource, globalConfig);
        generator.genCode("mpe_device_dbtable_info");
    }
}
