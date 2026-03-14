package cn.geoair.comp.message.converter.jts.mybatis.auto;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("cn.geoair.comp.message.converter.jts.mybatis")
@AutoConfigureBefore(name = { "tk.mybatis.mapper.autoconfigure.MapperAutoConfiguration",
		"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration" })
public class GirMybatisJtsAutoConfiguration {

}
