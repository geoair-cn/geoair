package cn.geoair.comp.message.converter.jts.jackson.config;

import cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.net.NetPGGeometryModule;
import cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.org.OrgPGGeometryModule;
import cn.geoair.map.dynamic.tools.convert.GirPostGisTran;
import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class JtsJacksonAutoConfig implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(JtsJacksonAutoConfig.class);

	@Autowired(required = false)
	private List<ObjectMapper> objectMappers;

	@Override
	public void afterPropertiesSet() {
		if (objectMappers != null) {
			for (ObjectMapper objectMapper : objectMappers) {
				objectMapper.registerModule(new JtsModule());
				log.debug("JtsJacksonAutoConfig 注册");
				if (GirPostGisTran.isNetConvert()) {
					objectMapper.registerModule(new NetPGGeometryModule());
					log.debug("NetPGGeometryModule 注册");
				}
				if (GirPostGisTran.isOrgConvert()) {
					objectMapper.registerModule(new OrgPGGeometryModule());
					log.debug("OrgPGGeometryModule 注册");
				}
			}
		}
	}

}
